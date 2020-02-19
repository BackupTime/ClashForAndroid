package com.github.kr328.clash

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import com.github.kr328.clash.core.Global
import com.github.kr328.clash.core.utils.Log
import com.github.kr328.clash.preference.UiSettings
import com.github.kr328.clash.remote.Broadcasts
import com.github.kr328.clash.remote.Remote
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import java.util.*

@Suppress("unused")
class MainApplication : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        Global.init(this)
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize AppCenter
        if (BuildConfig.APP_CENTER_KEY.isNotEmpty() && !BuildConfig.DEBUG) {
            AppCenter.start(
                this,
                BuildConfig.APP_CENTER_KEY,
                Analytics::class.java, Crashes::class.java
            )
        }

        Remote.init(this)
        Broadcasts.init(this)
    }
}