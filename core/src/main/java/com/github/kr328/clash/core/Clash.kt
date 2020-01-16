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
import java.io.InputStream
import java.lang.IllegalStateException
import java.util.concurrent.BrokenBarrierException
import java.util.concurrent.CompletableFuture

class Clash(
    context: Context,
    private val listener: (ProcessEvent) -> Unit
) {
    class Poll(private val poll: EventPoll) {
        fun stop() {
            poll.stop()
        }
    }

    private var currentProcess = ProcessEvent.STOPPED

    init {
        val country = context.assets.open("Country.mmdb")
            .use(InputStream::readBytes)

        Bridge.loadMMDB(country)
    }

    fun getCurrentProcessStatus(): ProcessEvent {
        return currentProcess
    }

    fun start() {
        if (currentProcess == ProcessEvent.STARTED)
            return

        currentProcess = ProcessEvent.STARTED

        listener(currentProcess)

        Bridge.reset()
    }

    fun stop() {
        if (currentProcess == ProcessEvent.STOPPED)
            return

        currentProcess = ProcessEvent.STOPPED

        listener(currentProcess)

        Bridge.reset()
    }

    fun startTunDevice(
        fd: Int,
        mtu: Int,
        dns: String
    ) {
        enforceStarted()

        Bridge.startTunDevice(fd.toLong(), mtu.toLong(), dns)
    }

    fun stopTunDevice() {
        Bridge.stopTunDevice()
    }

    fun loadProfile(path: String, baseDir: String): CompletableFuture<Unit> {
        enforceStarted()

        return DoneCallbackImpl().apply {
            Bridge.loadProfileFile(path, baseDir, this)
        }
    }

    fun downloadProfile(url: String, output: String, baseDir: String): CompletableFuture<Unit> {
        return DoneCallbackImpl().apply {
            Bridge.downloadProfileAndCheck(url, output, baseDir, this)
        }
    }

    fun saveProfile(data: ByteArray, output: String, baseDir: String): CompletableFuture<Unit> {
        return DoneCallbackImpl().apply {
            Bridge.saveProfileAndCheck(data, output, baseDir, this)
        }
    }

    fun moveProfile(source: String, target: String, baseDir: String): CompletableFuture<Unit> {
        return DoneCallbackImpl().apply {
            Bridge.moveProfileAndCheck(source, target, baseDir, this)
        }
    }

    fun queryProxyGroups(): List<ProxyGroup> {
        enforceStarted()

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

    fun startUrlTest(name: String): CompletableFuture<Unit> {
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

    private fun enforceStarted() {
        check(currentProcess != ProcessEvent.STOPPED) { "Clash Stopped" }
    }
}