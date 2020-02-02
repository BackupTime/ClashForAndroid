package com.github.kr328.clash.remote

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.github.kr328.clash.core.event.LogEvent
import com.github.kr328.clash.core.model.General
import com.github.kr328.clash.core.model.ProxyGroup
import com.github.kr328.clash.service.ClashManagerService
import com.github.kr328.clash.service.IClashManager
import com.github.kr328.clash.service.data.ClashProfileEntity
import com.github.kr328.clash.service.ipc.IStreamCallback
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

class ClashClient(private val service: IClashManager) {
    companion object {
        var clashInstanceChannel = Channel<ClashClient>()

        private val connection = object : ServiceConnection {
            var instance: ClashClient? = null
            var job: Job? = null

            override fun onServiceDisconnected(name: ComponentName?) {
                job?.cancel()
                instance?.close()
                instance = null
            }

            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                if (service != null)
                    instance = ClashClient(IClashManager.Stub.asInterface(service))

                job = GlobalScope.launch {
                    while (isActive) {
                        val clash = instance ?: return@launch
                        clashInstanceChannel.send(clash)
                    }
                }
            }
        }

        fun init(application: Application) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    application.bindService(
                        Intent(application, ClashManagerService::class.java),
                        connection,
                        Context.BIND_AUTO_CREATE
                    )
                }

                override fun onStop(owner: LifecycleOwner) {
                    application.unbindService(connection)
                }
            })
        }
    }

    private val openedChannel: MutableList<Channel<*>> = mutableListOf()

    suspend fun setSelectProxy(name: String, proxy: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext service.setSelectProxy(name, proxy)
    }

    suspend fun startHealthCheck(group: String) = withContext(Dispatchers.IO) {
        CompletableDeferred<Unit>().apply {
            service.startHealthCheck(group, object : IStreamCallback.Default() {
                override fun complete() {
                    this@apply.complete(Unit)
                }

                override fun completeExceptionally(reason: String?) {
                    this@apply.completeExceptionally(RemoteException(reason))
                }
            })
        }
    }.await()

    suspend fun queryAllProxyGroups(): Array<ProxyGroup> = withContext(Dispatchers.IO) {
        service.queryAllProxies()
    }

    suspend fun queryProfiles(): Array<ClashProfileEntity> = withContext(Dispatchers.IO) {
        service.queryAllProfiles()
    }

    suspend fun queryGeneral(): General = withContext(Dispatchers.IO) {
        service.queryGeneral()
    }

    suspend fun openLogChannel(): ReceiveChannel<LogEvent> = withContext(Dispatchers.IO) {
        LogChannel().apply {
            service.openLogEvent(createCallback())
        }
    }

    suspend fun queryBandwidth(): Long =
        withContext(Dispatchers.IO) {
            service.queryBandwidth()
        }

    fun close() {
        for (channel in openedChannel)
            channel.cancel()
    }
}