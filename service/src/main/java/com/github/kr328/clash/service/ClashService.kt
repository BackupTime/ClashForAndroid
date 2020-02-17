package com.github.kr328.clash.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.IBinder
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.service.data.ClashDatabase
import com.github.kr328.clash.service.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class ClashService : BaseService() {
    companion object {
        const val INTENT_EXTRA_START_TUN =
            "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.extra.start.tun"

        var isServiceRunning = false
    }

    private val service = this
    private lateinit var notification: ClashNotification
    private var stopReason: String? = null
    private val reloadChannel = Channel<Unit>(Channel.CONFLATED)
    private val settings: Settings by lazy { Settings(ClashManager(this, this)) }
    private val profileObserver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.`package` != packageName)
                return
            reloadChannel.offer(Unit)
        }
    }

    override fun onCreate() {
        super.onCreate()

        notification = ClashNotification(service, settings.get(Settings.NOTIFICATION_REFRESH))

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

        registerReceiver(profileObserver, IntentFilter(Intents.INTENT_ACTION_PROFILE_CHANGED))

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

        unregisterReceiver(profileObserver)

        isServiceRunning = false

        super.onDestroy()
    }

    private suspend fun reloadProfile() {
        val active = ClashDatabase.getInstance(service).openClashProfileDao()
            .queryActiveProfile() ?: return stopSelf("Empty active profile")

        try {
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
        }
    }

    private fun stopSelf(reason: String) {
        stopReason = reason
        stopSelf()
    }
}