package com.github.kr328.clash.core

import android.content.Context
import bridge.Bridge
import bridge.EventPoll
import com.github.kr328.clash.core.event.BandwidthEvent
import com.github.kr328.clash.core.event.LogEvent
import com.github.kr328.clash.core.event.ProcessEvent
import com.github.kr328.clash.core.event.TrafficEvent
import com.github.kr328.clash.core.model.General
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxyGroup
import com.github.kr328.clash.core.transact.ProxyCollectionImpl
import com.github.kr328.clash.core.transact.ProxyGroupCollectionImpl
import java.io.InputStream
import java.lang.IllegalStateException
import java.util.concurrent.CompletableFuture

class Clash(
    context: Context,
    private val listener: (ProcessEvent) -> Unit
) {
    companion object {
        const val CLASH_DIR = "clash"

        const val DEFAULT_URL_TEST_TIMEOUT = 5000
        const val DEFAULT_URL_TEST_URL = "https://www.gstatic.com/generate_204"
    }

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
        if ( currentProcess == ProcessEvent.STARTED )
            return

        currentProcess = ProcessEvent.STARTED

        listener(currentProcess)

        Bridge.reset()
    }

    fun stop() {
        if ( currentProcess == ProcessEvent.STOPPED )
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

    fun loadProfile(path: String, baseDir: String) {
        enforceStarted()

        Bridge.loadProfileFile(path, baseDir)
    }

    fun downloadProfile(url: String, output: String) {
        Bridge.downloadProfileAndCheck(url, output)
    }

    fun saveProfile(data: ByteArray, output: String) {
        Bridge.saveProfileAndCheck(data, output)
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
        val future = CompletableFuture<Unit>()

        Bridge.startUrlTest(name) {
            future.complete(Unit)
        }

        return future
    }

    fun queryGeneral(): General {
        val t = Bridge.queryGeneral()

        return General(General.Mode.fromString(t.mode),
            t.httpPort.toInt(), t.socksPort.toInt(), t.redirectPort.toInt())
    }

    fun pollTraffic(onEvent: (TrafficEvent) -> Unit): Poll {
        return Poll(Bridge.pollTraffic { down, up ->
            onEvent(TrafficEvent(down, up))
        })
    }

    fun pollBandwidth(onEvent: (BandwidthEvent) -> Unit): Poll {
        return Poll(Bridge.pollBandwidth {
            onEvent(BandwidthEvent(it))
        })
    }

    fun pollLogs(onEvent: (LogEvent) -> Unit): Poll {
        return Poll(Bridge.pollLogs { level, payload ->
            onEvent(LogEvent(LogEvent.Level.fromString(level), payload))
        })
    }

    private fun enforceStarted() {
        if ( currentProcess == ProcessEvent.STOPPED )
            throw IllegalStateException("Clash Stopped")
    }
}