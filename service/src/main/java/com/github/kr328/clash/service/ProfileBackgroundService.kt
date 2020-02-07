package com.github.kr328.clash.service

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.RemoteException
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.kr328.clash.service.data.ClashDatabase
import com.github.kr328.clash.service.data.ClashProfileDao
import com.github.kr328.clash.service.ipc.IStreamCallback
import com.github.kr328.clash.service.ipc.ParcelableContainer
import com.github.kr328.clash.service.transact.ProfileRequest
import com.github.kr328.clash.service.util.RandomUtils
import com.github.kr328.clash.service.util.componentName
import com.github.kr328.clash.service.util.intent
import com.github.kr328.clash.service.util.timeout
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select

class ProfileBackgroundService : BaseService() {
    companion object {
        private const val SERVICE_STATUS_CHANNEL = "profile_service_status"
        private const val SERVICE_RESULT_CHANNEL = "profile_service_result"
        private const val SERVICE_NOTIFICATION_ID = 10000
    }

    private val channel = Channel<ProfileRequest>(2)
    private val queue = mutableListOf<Deferred<ProfileRequest>>()
    private val profiles: ClashProfileDao by lazy {
        ClashDatabase.getInstance(this).openClashProfileDao()
    }
    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {}

        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = IProfileService.Stub.asInterface(binder) ?: return stopSelf()

            launch {
                while (isActive) {
                    val timeout = timeout(1000 * 60L)

                    select<Unit> {
                        channel.onReceive {
                            val deferred = CompletableDeferred<ProfileRequest>()

                            it.withCallback(object : IStreamCallback.Stub() {
                                override fun complete() {
                                    deferred.complete(it)
                                }

                                override fun completeExceptionally(reason: String?) {
                                    deferred.completeExceptionally(RemoteException(reason))
                                }

                                override fun send(data: ParcelableContainer?) {}
                            })

                            service.enqueueRequest(it)

                            queue.add(deferred)
                        }
                        if (queue.isNotEmpty()) {
                            for (task in queue) {
                                task.onAwait {
                                    queue.remove(task)
                                }
                            }
                        } else {
                            timeout.onJoin {
                                stopSelf()
                                cancel()
                            }
                        }
                    }

                    timeout.cancel()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        bindService(ProfileProcessService::class.intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        unbindService(connection)

        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        val request =
            intent?.getParcelableExtra<ProfileRequest>(Intents.INTENT_EXTRA_PROFILE_REQUEST)
                ?: return START_NOT_STICKY

        channel.offer(request)

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return Binder()
    }

    private fun resetProfileUpdateAlarm() {
        for (entity in profiles.queryProfiles()) {
            if (entity.updateInterval <= 0) continue

            val nextRequest =
                ProfileRequest().action(ProfileRequest.Action.UPDATE_OR_CREATE)
                    .withId(entity.id)

            requireNotNull(getSystemService(AlarmManager::class.java)).set(
                AlarmManager.RTC,
                entity.lastUpdate + entity.updateInterval,
                PendingIntent.getBroadcast(
                    this,
                    RandomUtils.nextInt(),
                    Intent(Intents.INTENT_ACTION_PROFILE_ENQUEUE_REQUEST)
                        .setComponent(ProfileRequestReceiver::class.componentName)
                        .putExtra(Intents.INTENT_EXTRA_PROFILE_REQUEST, nextRequest),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return

        NotificationManagerCompat.from(this).createNotificationChannels(
            listOf(
                NotificationChannel(
                    SERVICE_STATUS_CHANNEL,
                    getText(R.string.profile_service_status_channel),
                    NotificationManager.IMPORTANCE_LOW
                ),
                NotificationChannel(
                    SERVICE_RESULT_CHANNEL,
                    getText(R.string.profile_service_result),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        )
    }

    private fun createServiceNotification(content: CharSequence): Notification {
        return NotificationCompat.Builder(this, SERVICE_STATUS_CHANNEL)
            .setContentTitle(getText(R.string.profile_service_status_title))
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_updating)
            .setOnlyAlertOnce(true)
            .setProgress(Int.MAX_VALUE, 0, true)
            .build()
    }

    private fun createResultNotification(success: Boolean, content: CharSequence): Notification {
        return NotificationCompat.Builder(this, SERVICE_RESULT_CHANNEL)
            .setContentTitle(getText(R.string.profile_process_result))
            .setContentText(content)
            .setSmallIcon(if (success) R.drawable.ic_update_completed else R.drawable.ic_update_failure)
            .build()
    }

    private fun updateNotificationWaiting() {
        startForeground(
            SERVICE_NOTIFICATION_ID,
            createServiceNotification(getText(R.string.profile_service_status_waiting))
        )
    }

    private fun updateNotificationUpdating(profileName: String) {
        createServiceNotification(
            getString(
                R.string.profile_service_status_updating,
                profileName
            )
        )
    }

    private fun notifyResultNotification(success: Boolean, content: CharSequence) {
        NotificationManagerCompat.from(this)
            .notify(RandomUtils.nextInt(), createResultNotification(success, content))
    }
}