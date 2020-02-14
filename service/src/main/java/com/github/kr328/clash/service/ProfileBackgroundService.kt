package com.github.kr328.clash.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Build
import android.os.IBinder
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class ProfileBackgroundService : BaseService() {
    companion object {
        private const val SERVICE_STATUS_CHANNEL = "profile_service_status"
        private const val SERVICE_RESULT_CHANNEL = "profile_service_result"
        private const val SERVICE_NOTIFICATION_ID_BASE = 10000
    }

    private val channel = Channel<ProfileRequest>(2)
    private val queue = mutableListOf<CompletableDeferred<ProfileRequest>>()
    private val profiles: ClashProfileDao by lazy {
        ClashDatabase.getInstance(this).openClashProfileDao()
    }
    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            channel.close()
            stopSelf()
        }

        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = IProfileService.Stub.asInterface(binder) ?: return stopSelf()

            startProfileProcessor(service)
        }
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannels()

        startForeground()

        bindService(ProfileService::class.intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()

        unbindService(connection)

        stopForeground(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            Intents.INTENT_ACTION_PROFILE_ENQUEUE_REQUEST -> {
                val request =
                    intent.getParcelableExtra<ProfileRequest>(Intents.INTENT_EXTRA_PROFILE_REQUEST)
                        ?: return START_NOT_STICKY
                channel.offer(request)
            }
            Intents.INTENT_ACTION_PROFILE_SETUP -> {
                launch {
                    resetProfileUpdateAlarm()
                }
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return Binder()
    }

    private fun startProfileProcessor(service: IProfileService) = launch {
        while (isActive) {
            val timeout = timeout(1000 * 30L)

            select<Unit> {
                channel.onReceive {
                    val deferred = CompletableDeferred<ProfileRequest>()
                    val originalCallback = it.callback

                    it.withCallback(object : IStreamCallback.Stub() {
                        override fun complete() {
                            originalCallback?.complete()
                            deferred.complete(it)

                            launch {
                                updateUpdateComplete(it.id)
                            }
                        }

                        override fun completeExceptionally(reason: String?) {
                            originalCallback?.completeExceptionally(reason)
                            deferred.complete(it)

                            launch {
                                updateUpdateFailure(it.id, reason ?: "Unknown")
                            }
                        }

                        override fun send(data: ParcelableContainer?) {
                            originalCallback?.send(data)

                            launch {
                                updateUpdating(it.id)
                            }
                        }
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

    private suspend fun resetProfileUpdateAlarm() {
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
                    getText(R.string.profile_status_channel),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        )
    }

    private fun startForeground() {
        val notification = NotificationCompat.Builder(this, SERVICE_STATUS_CHANNEL)
            .setContentTitle(getText(R.string.profile_service_status_title))
            .setSmallIcon(R.drawable.ic_updating)
            .setOnlyAlertOnce(true)
            .build()

        startForeground(SERVICE_NOTIFICATION_ID_BASE, notification)
    }

    private suspend fun updateUpdating(id: Long) {
        val notificationId = ((id + 1) % (Int.MAX_VALUE - SERVICE_NOTIFICATION_ID_BASE)).toInt()
        val entity = profiles.queryProfileById(id) ?: return

        val notification = NotificationCompat.Builder(this, SERVICE_RESULT_CHANNEL)
            .setContentTitle(getText(R.string.profile_status_title))
            .setContentText(getString(R.string.profile_status_updating, entity.name))
            .setSmallIcon(R.drawable.ic_update_normal)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()

        NotificationManagerCompat.from(this)
            .notify(SERVICE_NOTIFICATION_ID_BASE + notificationId, notification)
    }

    private suspend fun updateUpdateComplete(id: Long) {
        val notificationId = ((id + 1) % (Int.MAX_VALUE - SERVICE_NOTIFICATION_ID_BASE)).toInt()
        val entity = profiles.queryProfileById(id)

        if (entity == null) {
            NotificationManagerCompat.from(this).cancel(notificationId)
            return
        }

        val notification = NotificationCompat.Builder(this, SERVICE_RESULT_CHANNEL)
            .setContentTitle(getText(R.string.profile_status_title))
            .setContentText(getString(R.string.profile_status_update_completed, entity.name))
            .setSmallIcon(R.drawable.ic_update_normal)
            .setOnlyAlertOnce(true)
            .build()

        NotificationManagerCompat.from(this)
            .notify(SERVICE_NOTIFICATION_ID_BASE + notificationId, notification)
    }

    private suspend fun updateUpdateFailure(id: Long, reason: String) {
        val notificationId = ((id + 1) % (Int.MAX_VALUE - SERVICE_NOTIFICATION_ID_BASE)).toInt()
        val entity = profiles.queryProfileById(id)

        if (entity == null) {
            NotificationManagerCompat.from(this).cancel(notificationId)
            return
        }

        val notification = NotificationCompat.Builder(this, SERVICE_RESULT_CHANNEL)
            .setContentTitle(getString(R.string.profile_status_update_failure, entity.name))
            .setContentText(reason)
            .setSmallIcon(R.drawable.ic_update_normal)
            .setOnlyAlertOnce(true)
            .build()

        NotificationManagerCompat.from(this)
            .notify(SERVICE_NOTIFICATION_ID_BASE + notificationId, notification)
    }
}