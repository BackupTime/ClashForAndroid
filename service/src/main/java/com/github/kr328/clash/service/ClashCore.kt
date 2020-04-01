package com.github.kr328.clash.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import androidx.core.content.getSystemService
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.utils.Log
import com.github.kr328.clash.service.data.ClashDatabase
import com.github.kr328.clash.service.settings.ServiceSettings
import com.github.kr328.clash.service.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select

class ClashCore(private val service: Service) : CoroutineScope by MainScope() {
    companion object {
        private const val CALL_GC_PERIOD = 300
    }

    private var stopReason: String? = null
    private val settings = ServiceSettings(service)
    private val database = ClashDatabase.getInstance(service)
    private val notification = ClashNotification(service)
    private val reloadChannel = Channel<Unit>(Channel.CONFLATED)
    private val screenChannel = Channel<Boolean>(Channel.CONFLATED)
    private val receivers = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON ->
                    screenChannel.offer(true)
                Intent.ACTION_SCREEN_OFF ->
                    screenChannel.offer(false)
                Intents.INTENT_ACTION_PROFILE_CHANGED ->
                    intent.enforceSelfPackage {
                        reloadChannel.offer(Unit)
                    }
                Intents.INTENT_ACTION_NETWORK_CHANGED ->
                    intent.enforceSelfPackage {
                        reloadChannel.offer(Unit)
                    }
                Intents.INTENT_ACTION_REQUEST_STOP ->
                    intent.enforceSelfPackage {
                        stopSelf(null)
                    }
            }
        }
    }

    fun start() {
        service.registerReceiver(receivers, IntentFilter().apply {
            addAction(Intents.INTENT_ACTION_PROFILE_CHANGED)
            addAction(Intents.INTENT_ACTION_REQUEST_STOP)
            addAction(Intents.INTENT_ACTION_NETWORK_CHANGED)

            if (settings.get(ServiceSettings.NOTIFICATION_REFRESH)) {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
        })

        broadcastClashStarted(service)

        Clash.start()

        ServiceStatusProvider.serviceRunning = true

        reloadChannel.offer(Unit)

        launch {
            val ticker = Channel<Unit>()
            var refreshCount = 0
            var enableRefresh = settings.get(ServiceSettings.NOTIFICATION_REFRESH)
                    && service.getSystemService<PowerManager>()!!.isInteractive

            launch {
                while (isActive) {
                    ticker.send(Unit)

                    delay(1000)
                }
            }

            while (isActive) {
                select<Unit> {
                    reloadChannel.onReceive {
                        reload()
                    }
                    screenChannel.onReceive {
                        enableRefresh = it

                        Log.i("Clash Notification Status $it")
                    }
                    if (enableRefresh) {
                        ticker.onReceive {
                            notification.update()

                            refreshCount++

                            if (refreshCount > CALL_GC_PERIOD) {
                                refreshCount = 0
                                System.gc()
                            }
                        }
                    }
                }
            }
        }
    }

    fun destroy() {
        cancel()

        service.unregisterReceiver(receivers)

        reloadChannel.close()

        notification.destroy()

        broadcastClashStopped(service, stopReason)

        Clash.stopTunDevice()
        Clash.stop()

        ServiceStatusProvider.serviceRunning = false
    }

    private fun stopSelf(reason: String?) {
        stopReason = reason

        service.stopSelf()

        Clash.stopTunDevice()
    }

    private suspend fun reload() {
        try {
            val active = database.openClashProfileDao()
                .queryActiveProfile() ?: return stopSelf("Active Profile not Found")

            Clash.loadProfile(
                resolveProfile(active.id),
                resolveBase(active.id)
            ).await()

            ClashDatabase.getInstance(service).openClashProfileProxyDao()
                .querySelectedForProfile(active.id).forEach {
                    Clash.setSelectedProxy(it.proxy, it.selected)
                }

            ServiceStatusProvider.currentProfile = active.name

            notification.setProfile(active.name)

            if (!settings.get(ServiceSettings.NOTIFICATION_REFRESH))
                notification.updateBase()

            broadcastProfileLoaded(service)
        } catch (e: Exception) {
            stopSelf(e.message ?: "Unknown")
        }
    }
}