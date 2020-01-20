package com.github.kr328.clash.core

import android.content.Context
import bridge.Bridge
import bridge.EventPoll
import com.github.kr328.clash.core.event.*
import com.github.kr328.clash.core.model.General
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxyGroup
import com.github.kr328.clash.core.transact.DoneCallbackImpl
import com.github.kr328.clash.core.transact.ProxyCollectionImpl
import com.github.kr328.clash.core.transact.ProxyGroupCollectionImpl
import java.io.File
import java.io.InputStream
import java.lang.IllegalStateException
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.CompletableFuture

object Clash{
    private var initialized = false

    class Poll(private val poll: EventPoll) {
        fun stop() {
            poll.stop()
        }
    }

    fun initialize(context: Context) {
        if ( initialized )
            return
        initialized = true

        val country = context.assets.open("Country.mmdb")
            .use(InputStream::readBytes)

        Bridge.loadMMDB(country)
    }

    fun start() {
        Bridge.reset()
    }

    fun stop() {
        Bridge.reset()
    }

    fun startTunDevice(
        fd: Int,
        mtu: Int,
        dns: String
    ) {
        Bridge.startTunDevice(fd.toLong(), mtu.toLong(), dns)
    }

    fun stopTunDevice() {
        Bridge.stopTunDevice()
    }

    fun loadProfile(path: File, baseDir: File): CompletableFuture<Unit> {
        return DoneCallbackImpl().apply {
            Bridge.loadProfileFile(path.absolutePath, baseDir.absolutePath, this)
        }
    }

    fun downloadProfile(url: String, output: File, baseDir: File): CompletableFuture<Unit> {
        return DoneCallbackImpl().apply {
            Bridge.downloadProfileAndCheck(url, output.absolutePath, baseDir.absolutePath, this)
        }
    }

    fun saveProfile(data: ByteArray, output: File, baseDir: File): CompletableFuture<Unit> {
        return DoneCallbackImpl().apply {
            Bridge.saveProfileAndCheck(data, output.absolutePath, baseDir.absolutePath, this)
        }
    }

    fun moveProfile(source: File, target: File, baseDir: File): CompletableFuture<Unit> {
        return DoneCallbackImpl().apply {
            Bridge.moveProfileAndCheck(source.absolutePath, target.absolutePath, baseDir.absolutePath, this)
        }
    }

    fun queryProxyGroups(): List<ProxyGroup> {
        return ProxyGroupCollectionImpl().also { Bridge.queryAllProxyGroups(it) }
            .filterNotNull()
            .map { group ->
                ProxyGroup(group.name,
                    Proxy.Type.fromString(group.type),
                    group.delay,
                    group.current,
                    ProxyCollectionImpl().also { pc ->
                        group.queryAllProxies(pc)
                    }.filterNotNull().map {
                        Proxy(it.name, Proxy.Type.fromString(it.type), it.delay)
                    })
            }
    }

    fun setSelectedProxy(name: String, selected: String): Boolean {
        return Bridge.setSelectedProxy(name, selected)
    }

    fun startHealthCheck(name: String): CompletableFuture<Unit> {
        return DoneCallbackImpl().apply {
            Bridge.startUrlTest(name, this)
        }
    }

    fun queryGeneral(): General {
        val t = Bridge.queryGeneral()

        return General(
            General.Mode.fromString(t.mode),
            t.httpPort.toInt(), t.socksPort.toInt(), t.redirectPort.toInt()
        )
    }

    fun openTrafficEvent(): EventStream<TrafficEvent> {
        return object: EventStream<TrafficEvent>() {
            val traffic = Bridge.pollTraffic { down, up ->
                send(TrafficEvent(down, up))
            }

            override fun onClose() {
                traffic.stop()
            }
        }
    }

    fun openBandwidthEvent(): EventStream<BandwidthEvent> {
        return object: EventStream<BandwidthEvent>() {
            val bandwidth = Bridge.pollBandwidth {
                send(BandwidthEvent(it))
            }

            override fun onClose() {
                bandwidth.stop()
            }
        }
    }

    fun openLogEvent(): EventStream<LogEvent> {
        return object: EventStream<LogEvent>() {
            val log = Bridge.pollLogs { level, payload ->
                send(LogEvent(LogEvent.Level.fromString(level), payload))
            }

            override fun onClose() {
                log.stop()
            }
        }
    }
}