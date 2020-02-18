package com.github.kr328.clash.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.service.data.ClashDatabase
import com.github.kr328.clash.service.settings.ServiceSettings
import com.github.kr328.clash.service.util.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class ClashService : BaseService() {
    companion object {
        const val INTENT_EXTRA_START_TUN =
            "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.extra.start.tun"

        var isServiceRunning = false
    }

    private val loadLock = Mutex()
    private val service = this
    private lateinit var notification: ClashNotification
    private var stopReason: String? = null
    private val reloadChannel = Channel<Unit>(Channel.CONFLATED)
    private val settings: ServiceSettings by lazy { ServiceSettings(this) }
    private val reloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.`package` != packageName)
                return
            reloadChannel.offer(Unit)
        }
    }

    override fun onCreate() {
        super.onCreate()

        notification = ClashNotification(service, settings.get(ServiceSettings.NOTIFICATION_REFRESH))

        launch {
            while (isActive) {
                reloadChannel.receive()

                reloadProfile()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (isServiceRunning)
            return START_NOT_STICKY

        isServiceRunning = true

        Clash.start()

        broadcastClashStarted(this)

        val startVpn = intent?.getBooleanExtra(INTENT_EXTRA_START_TUN, true) ?: true

        if (startVpn)
            startService(TunService::class.intent)

        registerReceiver(reloadReceiver, IntentFilter().apply {
            addAction(Intents.INTENT_ACTION_PROFILE_CHANGED)
            addAction(Intents.INTENT_ACTION_NETWORK_CHANGED)
        })

        reloadChannel.offer(Unit)

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return Binder()
    }

    override fun onDestroy() {
        cancel()

        Clash.stopTunDevice()
        Clash.stop()

        notification.destroy()

        broadcastClashStopped(this, stopReason)

        unregisterReceiver(reloadReceiver)

        isServiceRunning = false

        super.onDestroy()
    }

    private suspend fun reloadProfile() {
        if (!loadLock.tryLock())
            return

        try {
            val active = ClashDatabase.getInstance(service).openClashProfileDao()
                .queryActiveProfile() ?: return stopSelf("Empty active profile")

            Clash.loadProfile(
                resolveProfile(active.id),
                resolveBase(active.id)
            ).await()

            ClashDatabase.getInstance(service).openClashProfileProxyDao()
                .querySelectedForProfile(active.id).forEach {
                    Clash.setSelectedProxy(it.proxy, it.selected)
                }

            notification.setProfile(active.name)

            broadcastProfileLoaded(this, active)
        } catch (e: Exception) {
            stopSelf("Load profile failure")
        } finally {
            loadLock.unlock()
        }
    }

    private fun stopSelf(reason: String) {
        stopReason = reason
        stopSelf()
    }
}