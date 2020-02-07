package com.github.kr328.clash

import android.app.Application
import android.content.Context
import com.crashlytics.android.Crashlytics
import com.github.kr328.clash.core.Global
import com.github.kr328.clash.remote.Broadcasts
import com.github.kr328.clash.remote.ClashClient
import com.google.firebase.FirebaseApp
import io.fabric.sdk.android.Fabric

@Suppress("unused")
class MainApplication : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        Global.init(this)
    }

    override fun onCreate() {
        super.onCreate()

        runCatching {
            FirebaseApp.initializeApp(this)
        }
        runCatching {
            Fabric.with(this, Crashlytics())
        }

        ClashClient.init(this)
        Broadcasts.init(this)
    }
}