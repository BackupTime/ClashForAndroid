package com.github.kr328.clash.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.github.kr328.clash.core.Clash

class ClashService : Service() {
    private var clashCore: ClashCore? = null

    override fun onCreate() {
        super.onCreate()

        if ( ServiceStatusProvider.serviceRunning )
            return stopSelf()

        Clash.initialize(this)

        clashCore = ClashCore(this)

        clashCore?.start()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return Binder()
    }

    override fun onDestroy() {
        clashCore?.destroy()
    }
}