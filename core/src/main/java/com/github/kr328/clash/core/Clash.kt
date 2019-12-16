package com.github.kr328.clash.core

import android.content.Context
import bridge.Bridge
import bridge.EventPoll
import bridge.Logs
import com.github.kr328.clash.core.event.BandwidthEvent
import com.github.kr328.clash.core.event.LogEvent
import com.github.kr328.clash.core.event.ProcessEvent
import com.github.kr328.clash.core.event.TrafficEvent
import com.github.kr328.clash.core.model.General
import com.github.kr328.clash.core.model.Proxy
import java.io.FileOutputStream
import java.lang.Exception
import java.lang.IllegalStateException

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
        val home = context.filesDir.resolve(CLASH_DIR)
        val countryDatabase = home.resolve("Country.mmdb")

        home.resolve("ui").mkdirs()

        if ( context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime >
            countryDatabase.lastModified() ) {
            FileOutputStream(countryDatabase).use { output ->
                context.assets.open("Country.mmdb").use { input ->
                    input.copyTo(output)
                }
            }
        }

        Bridge.init(home.absolutePath)
    }

    fun getCurrentProcessStatus(): ProcessEvent {
        return currentProcess
    }

    fun start() {
        if ( currentProcess == ProcessEvent.STARTED )
            return

        currentProcess = ProcessEvent.STARTED

        listener(currentProcess)

        loadDefault()
    }

    fun stop() {
        if ( currentProcess == ProcessEvent.STOPPED )
            return

        currentProcess = ProcessEvent.STOPPED

        listener(currentProcess)

        loadDefault()
    }

    fun startTunDevice(fd: Int, mtu: Int, gateway: String, dns: String, onSocket: (Int) -> Unit) {
        enforceStarted()

        Bridge.startTunDevice(fd.toLong(), mtu.toLong(), gateway, dns) {
            onSocket(it.toInt())
        }
    }

    fun stopTunDevice() {
        Bridge.stopTunDevice()
    }

    fun loadProfile(path: String) {
        enforceStarted()

        Bridge.loadProfileFile(path)
    }

    fun checkProfileValid(data: String): String? {
        return try {
            Bridge.checkProfileValid(data)
            null
        }
        catch (e: Exception) {
            e.message
        }
    }

    fun queryProxies(): List<Proxy> {
        enforceStarted()

        val list = Bridge.queryAllProxies()
        val result = mutableListOf<Proxy>()

        for (i in 0 until list.proxiesLength) {
            val p = list.getProxiesElement(i)
            val all = mutableListOf<String>()

            for (index in 0 until p.allLength) {
                all.add(p.getAllElement(index))
            }

            result.add(
                Proxy(
                    p.name,
                    Proxy.Type.fromString(p.type),
                    p.now,
                    all,
                    if ( p.delay < Short.MAX_VALUE ) p.delay else 0
                )
            )
        }

        return result
    }

    fun setSelectedProxy(name: String, selected: String): Boolean {
        return Bridge.setSelectedProxy(name, selected)
    }

    fun startUrlTest(name: String, callback: (String, Long) -> Unit) {
        Bridge.startUrlTest(name, DEFAULT_URL_TEST_URL, DEFAULT_URL_TEST_TIMEOUT.toLong()) { n, delay ->
            callback(n, delay)
        }
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

    private fun loadDefault() {
        Bridge.loadProfileDefault()
    }

    private fun enforceStarted() {
        if ( currentProcess == ProcessEvent.STOPPED )
            throw IllegalStateException("Clash Stopped")
    }
}