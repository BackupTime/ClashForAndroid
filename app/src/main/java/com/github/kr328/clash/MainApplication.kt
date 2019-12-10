package com.github.kr328.clash

import android.app.Application
import android.content.Context
import com.crashlytics.android.Crashlytics
import com.github.kr328.clash.core.Constants
import com.github.kr328.clash.core.utils.Log
import com.google.firebase.FirebaseApp
import io.fabric.sdk.android.Fabric

class MainApplication : Application() {
    companion object {
        const val KEY_PROXY_MODE = "key_proxy_mode"
        const val PROXY_MODE_VPN = "vpn"
        const val PROXY_MODE_PROXY_ONLY = "proxy_only"

        lateinit var instance: MainApplication
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        instance = this
    }

    override fun onCreate() {
        super.onCreate()

        runCatching {
            FirebaseApp.initializeApp(this)
        }
        runCatching {
            Fabric.with(this)
        }

        Log.handler = object: Log.LogHandler {
            override fun info(message: String, throwable: Throwable?) {
                android.util.Log.i(Constants.TAG, message, throwable)
            }

            override fun warn(message: String, throwable: Throwable?) {
                throwable?.also {
                    Crashlytics.logException(it)
                }

                android.util.Log.w(Constants.TAG, message, throwable)
            }

            override fun error(message: String, throwable: Throwable?) {
                throwable?.also {
                    Crashlytics.logException(it)
                }

                android.util.Log.e(Constants.TAG, message, throwable)
            }

            override fun wtf(message: String, throwable: Throwable?) {
                throwable?.also {
                    Crashlytics.logException(it)
                }

                android.util.Log.wtf(Constants.TAG, message, throwable)
            }

            override fun debug(message: String, throwable: Throwable?) {
                android.util.Log.d(Constants.TAG, message, throwable)
            }
        }
    }
}