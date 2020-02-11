package com.github.kr328.clash.remote

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.edit
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.github.kr328.clash.ApkBrokenActivity
import com.github.kr328.clash.Constants
import com.github.kr328.clash.core.utils.Log
import com.github.kr328.clash.service.ClashManagerService
import com.github.kr328.clash.service.IClashManager
import com.github.kr328.clash.service.IProfileService
import com.github.kr328.clash.service.ProfileService
import com.github.kr328.clash.service.util.intent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.zip.ZipFile

object Remote {
    var clash = Channel<ClashClient>()
    var profile = Channel<ProfileClient>()

    private var clashConnection: ClashConnection? = null
    private var profileConnection: ProfileConnection? = null

    class ClashConnection : ServiceConnection {
        private var instance: ClashClient? = null
        private var sender: Job? = null

        override fun onServiceDisconnected(name: ComponentName?) {
            sender?.cancel()
            instance = null
            sender = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service != null)
                instance = ClashClient(IClashManager.Stub.asInterface(service))

            sender = GlobalScope.launch {
                while (isActive) {
                    val client = instance ?: return@launch
                    clash.send(client)
                    Log.d("Clash Client sent")
                }
            }
        }
    }

    class ProfileConnection : ServiceConnection {
        private var instance: ProfileClient? = null
        private var sender: Job? = null

        override fun onServiceDisconnected(name: ComponentName?) {
            sender?.cancel()
            instance = null
            sender = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service != null)
                instance = ProfileClient(IProfileService.Stub.asInterface(service))

            sender = GlobalScope.launch {
                while (isActive) {
                    val client = instance ?: return@launch
                    profile.send(client)
                    Log.d("Profile Client sent")
                }
            }
        }
    }

    fun init(application: Application) {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                GlobalScope.launch {
                    if (!verifyApk(application)) {
                        application.startActivity(ApkBrokenActivity::class.intent)
                        return@launch
                    }

                    clashConnection = ClashConnection().apply {
                        application.bindService(
                            ClashManagerService::class.intent,
                            this,
                            Context.BIND_AUTO_CREATE
                        )
                    }

                    profileConnection = ProfileConnection().apply {
                        application.bindService(
                            ProfileService::class.intent,
                            this,
                            Context.BIND_AUTO_CREATE
                        )
                    }
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                clashConnection?.also(application::unbindService)
                profileConnection?.also(application::unbindService)

                clashConnection = null
                profileConnection = null
            }
        })
    }

    private suspend fun verifyApk(application: Application): Boolean {
        return withContext(Dispatchers.IO) {
            val sp = application.getSharedPreferences(
                Constants.PREFERENCE_NAME_APP,
                Context.MODE_PRIVATE
            )
            val pkg = application.packageManager.getPackageInfo(application.packageName, 0)

            if (sp.getLong(Constants.PREFERENCE_KEY_LAST_INSTALL, 0) == pkg.lastUpdateTime)
                return@withContext true

            val info = application.applicationInfo
            val sources =
                info.splitSourceDirs ?: arrayOf(info.sourceDir) ?: return@withContext false

            for (apk in sources) {
                if (ZipFile(apk).entries().asSequence().any { it.name.endsWith("libgojni.so") }) {
                    sp.edit {
                        putLong(Constants.PREFERENCE_KEY_LAST_INSTALL, pkg.lastUpdateTime)
                    }
                    return@withContext true
                }
            }
            return@withContext false
        }
    }
}