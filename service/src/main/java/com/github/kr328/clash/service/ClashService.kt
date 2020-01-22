package com.github.kr328.clash.service

import android.app.Service
import android.content.*
import android.os.Binder
import android.os.IBinder
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.service.data.ClashDatabase
import com.github.kr328.clash.service.util.DefaultThreadPool
import com.github.kr328.clash.service.util.sendBroadcastSelf

class ClashService : Service() {
    companion object {
        const val INTENT_EXTRA_START_TUN =
            "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.extra.start.tun"
    }

    private var stopReason: String? = null
    private lateinit var notification: ClashNotification
    private val tunConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {}
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            require(service != null)

            val tun = service.queryLocalInterface(TunService::class.java.name) as TunService

            tun.startTun().whenComplete { _, u ->
                if (u != null)
                    return@whenComplete stopSelf(u.message ?: "Start tun failure")

                notification.setVpn(true)
            }
        }
    }
    private val profileObserver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            reloadProfile()
        }
    }

    override fun onCreate() {
        super.onCreate()

        notification = ClashNotification(this)

        Clash.initialize(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        Clash.start()

        sendBroadcastSelf(Intent(Intents.INTENT_ACTION_CLASH_STARTED))

        val startVpn = intent?.getBooleanExtra(INTENT_EXTRA_START_TUN, true) ?: true

        if (startVpn)
            bindService(
                Intent(this, TunService::class.java)
                    .setAction(Intents.INTENT_ACTION_BIND_TUN_SERVICE),
                tunConnection,
                Context.BIND_AUTO_CREATE
            )

        reloadProfile()

        registerReceiver(profileObserver, IntentFilter(Intents.INTENT_ACTION_PROFILE_CHANGED))

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return Binder()
    }

    override fun onDestroy() {
        runCatching {
            unbindService(tunConnection)
        }

        Clash.stop()

        notification.destroy()

        sendBroadcastSelf(
            Intent(Intents.INTENT_ACTION_CLASH_STOPPED)
                .putExtra(Intents.INTENT_EXTRA_CLASH_STOP_REASON, stopReason)
        )

        unregisterReceiver(profileObserver)

        super.onDestroy()
    }

    private fun reloadProfile() {
        DefaultThreadPool.submit {
            val active = ClashDatabase.getInstance(this).openClashProfileDao().queryActiveProfile()
                ?: return@submit stopSelf("Empty active profile")

            Clash.loadProfile(
                filesDir.resolve(Constants.PROFILES_DIR).resolve(active.file),
                filesDir.resolve(Constants.CLASH_DIR).resolve(active.base)
            ).whenComplete { _, u ->
                if (u != null)
                    return@whenComplete stopSelf(u.message ?: "Load profile failure")
            }
        }
    }

    private fun stopSelf(reason: String) {
        stopReason = reason
        stopSelf()
    }
}