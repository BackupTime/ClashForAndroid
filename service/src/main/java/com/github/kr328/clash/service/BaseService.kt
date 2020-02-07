package com.github.kr328.clash.service

import android.app.Service
import com.github.kr328.clash.core.Clash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

abstract class BaseService : Service(), CoroutineScope by MainScope() {
    override fun onCreate() {
        super.onCreate()

        Clash.initialize(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        cancel()
    }
}