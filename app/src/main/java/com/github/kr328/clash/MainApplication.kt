package com.github.kr328.clash

import android.app.Application
import android.content.Context
import android.os.Build
import com.crashlytics.android.Crashlytics
import com.github.kr328.clash.core.Global
import com.github.kr328.clash.remote.Broadcasts
import com.github.kr328.clash.remote.ClashClient
import com.google.firebase.FirebaseApp
import io.fabric.sdk.android.Fabric
import java.security.MessageDigest

@Suppress("unused")
class MainApplication : Application() {
    companion object {
        const val KEY_PROXY_MODE = "key_proxy_mode"
        const val PROXY_MODE_VPN = "vpn"
        const val PROXY_MODE_PROXY_ONLY = "proxy_only"

        val userIdentifier: String by lazy {
            val archive =
                Global.application.packageManager.getPackageInfo(Global.application.packageName, 0)
            val encoder = MessageDigest.getInstance("md5")

            encoder.digest((Build.ID + archive.lastUpdateTime).toByteArray()).toHexString()
        }

        private fun ByteArray.toHexString(): String {
            return this.map {
                Integer.toHexString(it.toInt() and 0xff)
            }.joinToString(separator = "") {
                if (it.length < 2)
                    "0$it"
                else
                    it
            }
        }
    }

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

        Crashlytics.setUserIdentifier(userIdentifier)

        ClashClient.init(this)
        Broadcasts.init(this)
    }
}