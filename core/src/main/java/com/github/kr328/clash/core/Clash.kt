package com.github.kr328.clash.core

import android.content.Context
import bridge.Bridge
import com.github.kr328.clash.core.event.ProcessEvent
import com.github.kr328.clash.core.model.General
import com.github.kr328.clash.core.model.Proxy
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

    private var currentProcess = ProcessEvent.STOPPED

    init {
        val home = context.filesDir.resolve(CLASH_DIR)

        home.mkdirs()

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

    fun queryProxies(): List<Proxy> {
        enforceStarted()

        val list = Bridge.queryAllProxies()
        val result = mutableListOf<Proxy>()

        for (i in 0..list.proxiesLength) {
            val p = list.getProxiesElement(i)
            val all = mutableListOf<String>()

            for (index in 0..p.allLength) {
                all.add(p.getAllElement(index))
            }

            result.add(
                Proxy(
                    p.name,
                    Proxy.Type.fromString(p.type),
                    p.now,
                    all,
                    p.delay
                )
            )
        }

        return result
    }

    fun queryGeneral(): General {
        val t = Bridge.queryGeneral()

        return General(General.Mode.fromString(t.mode),
            t.httpPort.toInt(), t.socksPort.toInt(), t.redirectPort.toInt())
    }

    private fun loadDefault() {
        Bridge.loadProfileDefault()
    }

    private fun enforceStarted() {
        if ( currentProcess == ProcessEvent.STOPPED )
            throw IllegalStateException("Clash Stopped")
    }
}