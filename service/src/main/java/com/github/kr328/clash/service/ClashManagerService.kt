package com.github.kr328.clash.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class ClashManagerService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return ClashManager(this)
    }
}