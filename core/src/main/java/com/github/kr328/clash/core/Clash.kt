package com.github.kr328.clash.core

import android.os.ParcelFileDescriptor
import com.github.kr328.clash.common.Global
import com.github.kr328.clash.common.utils.Log
import com.github.kr328.clash.core.bridge.Bridge
import com.github.kr328.clash.core.event.LogEvent
import com.github.kr328.clash.core.model.General
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxyGroup
import com.github.kr328.clash.core.model.Traffic
import com.github.kr328.clash.core.transact.DoneCallbackImpl
import com.github.kr328.clash.core.transact.ProxyCollectionImpl
import com.github.kr328.clash.core.transact.ProxyGroupCollectionImpl
import kotlinx.coroutines.CompletableDeferred
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.InputStream
import kotlin.concurrent.thread

object Clash {
    private val logReceivers = mutableMapOf<String, (LogEvent) -> Unit>()

    init {
        val context = Global.application

        val bytes = context.assets.open("Country.mmdb")
            .use(InputStream::readBytes)

        Bridge.initialize(bytes, context.cacheDir.absolutePath, BuildConfig.VERSION_NAME)
        Bridge.reset()
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
        gateway: String,
        mirror: String,
        dns: String,
        onNewSocket: (Int) -> Boolean,
        onTunStop: () -> Unit
    ) {
        val sockets = ParcelFileDescriptor.createSocketPair()

        thread {
            val input = DataInputStream(ParcelFileDescriptor.AutoCloseInputStream(sockets[0]))
            val output = DataOutputStream(ParcelFileDescriptor.AutoCloseOutputStream(sockets[0]))

            runCatching {
                while (true) {
                    val s = input.readInt()

                    onNewSocket(s)

                    output.writeInt(s)
                }
            }

            onTunStop()

            Log.i("Tun Closed")

            sockets[0].close()
            sockets[1].close()
        }

        Bridge.startTunDevice(fd, mtu, gateway, mirror, dns, sockets[1].detachFd())
    }

    fun stopTunDevice() {
        Bridge.stopTunDevice()
    }

    fun setDnsOverride(dnsOverride: Boolean, appendNameservers: List<String>) {
        Bridge.setDnsOverride(dnsOverride, appendNameservers.joinToString(","))
    }

    fun loadProfile(path: File, baseDir: File): CompletableDeferred<Unit> {
        return DoneCallbackImpl().apply {
            Bridge.loadProfileFile(path.absolutePath, baseDir.absolutePath, this)
        }
    }

    fun downloadProfile(url: String, output: File, baseDir: File): CompletableDeferred<Unit> {
        return DoneCallbackImpl().apply {
            Bridge.downloadProfileAndCheck(url, output.absolutePath, baseDir.absolutePath, this)
        }
    }

    fun downloadProfile(fd: Int, output: File, baseDir: File): CompletableDeferred<Unit> {
        return DoneCallbackImpl().apply {
            Bridge.readProfileAndCheck(fd.toLong(), output.absolutePath, baseDir.absolutePath, this)
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

    fun startHealthCheck(name: String): CompletableDeferred<Unit> {
        return DoneCallbackImpl().apply {
            Bridge.startUrlTest(name, this)
        }
    }

    fun setProxyMode(mode: String) {
        Bridge.setProxyMode(mode)
    }

    fun queryGeneral(): General {
        return Bridge.queryGeneral()
    }

    fun querySpeed(): Traffic {
        return Bridge.querySpeed()
    }

    fun queryBandwidth(): Traffic {
        return Bridge.queryBandwidth()
    }

    fun registerLogReceiver(key: String, receiver: (LogEvent) -> Unit) {
        synchronized(logReceivers) {
            logReceivers[key] = receiver

            Bridge.setLogCallback(this::onLogEvent)
        }
    }

    fun unregisterLogReceiver(key: String) {
        synchronized(logReceivers) {
            logReceivers.remove(key)

            if (logReceivers.isEmpty())
                Bridge.setLogCallback(null)
        }
    }

    private fun onLogEvent(level: String, payload: String) {
        synchronized(logReceivers) {
            logReceivers.forEach {
                it.value(LogEvent(LogEvent.Level.fromString(level), payload))
            }
        }
    }
}