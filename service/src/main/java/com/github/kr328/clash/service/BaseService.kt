package com.github.kr328.clash.service

import android.app.Service
import android.content.Context
import android.content.res.Configuration
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.service.settings.ServiceSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import java.util.*

abstract class BaseService : Service(), CoroutineScope by MainScope() {
    lateinit var settings: ServiceSettings

    override fun attachBaseContext(base: Context?) {
        settings = ServiceSettings(base ?: return super.attachBaseContext(base))

        val language = settings.get(ServiceSettings.LANGUAGE)
        if ( language.isEmpty() )
            return super.attachBaseContext(base)
        val languageOverride = language.split("-")

        val configuration = Configuration()
        val localeOverride = if (languageOverride.size == 2)
            Locale(languageOverride[0], languageOverride[1])
        else
            Locale(languageOverride[0])

        configuration.setLocale(localeOverride)

        super.attachBaseContext(base.createConfigurationContext(configuration))
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