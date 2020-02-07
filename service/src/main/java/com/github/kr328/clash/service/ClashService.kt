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
    }

    private val service = this
    private val notification by lazy { ClashNotification(service) }
    private var stopReason: String? = null
    private val reloadChannel = Channel<Unit>(Channel.CONFLATED)
    private val profileObserver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            reloadChannel.offer(Unit)
        }
    }

    override fun onCreate() {
        super.onCreate()

        launch {
            while (isActive) {
                reloadChannel.receive()

                reloadProfile()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

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

        super.onDestroy()
    }

    private suspend fun reloadProfile() = withContext(Dispatchers.IO) {
        val active = ClashDatabase.getInstance(service).openClashProfileDao().queryActiveProfile()
            ?: return@withContext stopSelf("Empty active profile")

        try {
            Clash.loadProfile(
                profileDir.resolve(active.file),
                clashDir.resolve(active.base)
            ).await()

            notification.setProfile(active.name)
        } catch (e: Exception) {
            stopSelf("Load profile failure")
        }
    }

    private fun stopSelf(reason: String) {
        stopReason = reason
        stopSelf()
    }
}