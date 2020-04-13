package com.github.kr328.clash.service

import android.app.Service
import android.content.Context
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.service.settings.ServiceSettings
import com.github.kr328.clash.common.util.createLanguageConfigurationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

abstract class BaseService : Service(), CoroutineScope by MainScope() {
    lateinit var settings: ServiceSettings

    override fun attachBaseContext(base: Context?) {
        settings = ServiceSettings(base ?: return super.attachBaseContext(base))

        val language = settings.get(ServiceSettings.LANGUAGE)

        super.attachBaseContext(base.createLanguageConfigurationContext(language))
    }

    override fun onCreate() {
        super.onCreate()

        Clash.initialize(this)
    }

    override fun onDestroy() {
        super.onDestroy()

        cancel()
    }
}