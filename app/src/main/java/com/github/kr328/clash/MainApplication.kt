package com.github.kr328.clash

import android.app.Application
import android.content.Context
import android.os.Build
import com.crashlytics.android.Crashlytics
import com.github.kr328.clash.core.Constants
import com.github.kr328.clash.core.utils.ByteFormatter
import com.github.kr328.clash.core.utils.Log
import com.google.firebase.FirebaseApp
import io.fabric.sdk.android.Fabric
import java.io.File
import java.lang.Exception
import java.security.MessageDigest
import java.util.zip.ZipFile
import kotlin.concurrent.thread


class MainApplication : Application() {
    companion object {
        const val KEY_PROXY_MODE = "key_proxy_mode"
        const val PROXY_MODE_VPN = "vpn"
        const val PROXY_MODE_PROXY_ONLY = "proxy_only"

        val GOOGLE_PLAY_INSTALLER = listOf("com.android.vending", "com.google.android.feedback")
        const val CRASHLYTICS_FROM_PLAY_KEY = "install_from_google"
        const val CRASHLYTICS_SPLIT_APK_KEY = "split_apk"
        const val CRASHLYTICS_BASE_SIZE_KEY = "base_size"

        val userIdentifier: String by lazy {
            val archive = instance.packageManager.getPackageInfo(instance.packageName, 0)
            val encoder = MessageDigest.getInstance("md5")

            encoder.digest((Build.ID + archive.lastUpdateTime).toByteArray()).toHexString()
        }

        lateinit var instance: MainApplication

        private fun ByteArray.toHexString(): String {
            return this.map {
                Integer.toHexString(it.toInt() and 0xff)
            }.map {
                if (it.length < 2)
                    "0$it"
                else
                    it
            }.joinToString(separator = "")
        }
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
            Fabric.with(this, Crashlytics())
        }

        Crashlytics.setBool(CRASHLYTICS_FROM_PLAY_KEY, detectFromPlay())
        Crashlytics.setBool(CRASHLYTICS_SPLIT_APK_KEY, detectSplitArchive())
        Crashlytics.setString(CRASHLYTICS_BASE_SIZE_KEY, getBaseApkSize())
        Crashlytics.setUserIdentifier(userIdentifier)

        Log.handler = object : Log.LogHandler {
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

    private fun detectFromPlay(): Boolean {
        val installer = packageManager.getInstallerPackageName(packageName)
        return installer != null && GOOGLE_PLAY_INSTALLER.contains(installer)
    }

    private fun detectSplitArchive(): Boolean {
        val split = applicationInfo.splitSourceDirs
        return split != null && split.isNotEmpty()
    }

    private fun getBaseApkSize(): String {
        val size = File(applicationInfo.sourceDir).length()

        return ByteFormatter.byteToString(size)
    }
}