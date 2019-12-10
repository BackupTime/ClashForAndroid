package com.github.kr328.clash

import android.app.Application
import android.content.Context
import com.github.kr328.clash.core.Constants
import com.github.kr328.clash.core.utils.Log
import com.google.firebase.FirebaseApp

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

        try {
            FirebaseApp.initializeApp(this)
            Log.i("Registered")
        } catch (e: IllegalStateException) {
            Log.i("Already registered")
        }

        Log.handler = object: Log.LogHandler {
            override fun info(message: String, throwable: Throwable?) {
                android.util.Log.i(Constants.TAG, message, throwable)
            }

            override fun warn(message: String, throwable: Throwable?) {

            }

            override fun error(message: String, throwable: Throwable?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun wtf(message: String, throwable: Throwable?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun debug(message: String, throwable: Throwable?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }
    }
}