package com.github.kr328.clash.service

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.github.kr328.clash.service.clash.ClashRuntime
import com.github.kr328.clash.service.clash.module.CloseModule
import com.github.kr328.clash.service.clash.module.DynamicNotificationModule
import com.github.kr328.clash.service.clash.module.ReloadModule
import com.github.kr328.clash.service.clash.module.StaticNotificationModule
import com.github.kr328.clash.service.settings.ServiceSettings
import com.github.kr328.clash.service.util.broadcastClashStarted
import com.github.kr328.clash.service.util.broadcastClashStopped
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ClashService : BaseService() {
    private val service = this
    private val runtime = ClashRuntime()
    private var reason: String? = null

    override fun onCreate() {
        super.onCreate()

        if (ServiceStatusProvider.serviceRunning)
            return stopSelf()

        ServiceStatusProvider.serviceRunning = true

        StaticNotificationModule.createNotificationChannel(service)
        StaticNotificationModule.notifyLoadingNotification(service)

        launch {
            val settings = ServiceSettings(service)

            runtime.install(ReloadModule(service)) {
                onEmptyProfile {
                    reason = "No profile selected"

                    stopSelf()
                }
            }
            runtime.install(CloseModule(service)) {
                onClose {
                    reason = null

                    stopSelf()
                }
            }

            if (settings.get(ServiceSettings.NOTIFICATION_REFRESH))
                runtime.install(DynamicNotificationModule(service))
            else
                runtime.install(StaticNotificationModule(service))

            runtime.start()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        this.broadcastClashStarted()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return Binder()
    }

    override fun onDestroy() {
        runBlocking {
            ServiceStatusProvider.serviceRunning = false

            runtime.stop()

            service.broadcastClashStopped(reason)
        }

        super.onDestroy()
    }
}