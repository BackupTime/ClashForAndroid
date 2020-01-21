package com.github.kr328.clash.service

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import com.github.kr328.clash.core.Clash

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
                if (u != null) {
                    stopReason = u.message
                    stopSelf()
                    return@whenComplete
                }

                notification.setVpn(true)
            }
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

        sendBroadcast(Intent(Intents.INTENT_ACTION_CLASH_STARTED))

        val startVpn = intent?.getBooleanExtra(INTENT_EXTRA_START_TUN, true) ?: true

        if (startVpn)
            bindService(
                Intent(this, TunService::class.java)
                    .setAction(Intents.INTENT_ACTION_BIND_TUN_SERVICE),
                tunConnection,
                Context.BIND_AUTO_CREATE
            )

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

        sendBroadcast(
            Intent(Intents.INTENT_ACTION_CLASH_STOPPED)
                .putExtra(Intents.INTENT_ACTION_CLASH_STOP_REASON, stopReason)
        )

        super.onDestroy()
    }

    private fun reloadProfile() {

    }
}