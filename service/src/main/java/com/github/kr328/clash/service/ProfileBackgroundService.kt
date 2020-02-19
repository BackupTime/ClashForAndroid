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
import com.github.kr328.clash.service.util.UpdateUtils
import com.github.kr328.clash.service.util.componentName
import com.github.kr328.clash.service.util.intent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

class ProfileBackgroundService : BaseService() {
    companion object {
        private const val SERVICE_STATUS_CHANNEL = "profile_service_status"
        private const val SERVICE_RESULT_CHANNEL = "profile_service_result"
    }

    private val requestChannel = Channel<ProfileRequest>(2)
    private val responseChannel = Channel<ProfileRequest>(2)
    private val profiles: ClashProfileDao by lazy {
        ClashDatabase.getInstance(this).openClashProfileDao()
    }
    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            requestChannel.close()
            responseChannel.close()
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

        refreshStatusNotification(0)

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
                launch {
                    requestChannel.send(request)
                }
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
        val queue: MutableMap<Long, ProfileRequest> = mutableMapOf()

        do {
            select<Unit> {
                requestChannel.onReceive {
                    if ( !queue.containsKey(it.id) ) {
                        queue[it.id] = it

                        sendRequest(it, service)
                    }
                }
                responseChannel.onReceive {
                    queue.remove(it.id)
                }
            }

            refreshStatusNotification(queue.size)
        } while ( queue.isNotEmpty() )

        stopSelf()
    }

    private fun sendRequest(request: ProfileRequest, service: IProfileService) {
        val originalCallback = request.callback

        request.withCallback(object: IStreamCallback.Stub() {
            override fun complete() {
                originalCallback?.complete()

                launch {
                    responseChannel.send(request)

                    sendUpdateCompleted(request.id)
                }
            }

            override fun completeExceptionally(reason: String?) {
                originalCallback?.completeExceptionally(reason)

                launch {
                    responseChannel.send(request)

                    sendUpdateFailure(request.id, reason?: "Unknown")
                }
            }

            override fun send(data: ParcelableContainer?) {}
        })

        service.enqueueRequest(request)
    }

    private suspend fun resetProfileUpdateAlarm() {
        for (entity in profiles.queryProfiles()) {
            UpdateUtils.resetProfileUpdateAlarm(this, entity)
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

    private fun refreshStatusNotification(queueSize: Int) {
        val notification = NotificationCompat.Builder(this, SERVICE_STATUS_CHANNEL)
            .setContentTitle(getText(R.string.processing_profiles))
            .setContentText(getString(R.string.format_in_queue, queueSize))
            .setColor(getColor(R.color.colorAccentService))
            .setSmallIcon(R.drawable.ic_notification)
            .setOnlyAlertOnce(true)
            .build()

        startForeground(RandomUtils.nextInt(), notification)
    }

    private suspend fun sendUpdateCompleted(id: Long) {
        val entity = profiles.queryProfileById(id) ?: return

        val notification = NotificationCompat.Builder(this, SERVICE_RESULT_CHANNEL)
            .setContentTitle(getText(R.string.process_result))
            .setContentText(getString(R.string.format_update_complete, entity.name))
            .setColor(getColor(R.color.colorAccentService))
            .setSmallIcon(R.drawable.ic_notification)
            .setOnlyAlertOnce(true)
            .build()

        NotificationManagerCompat.from(this)
            .notify(RandomUtils.nextInt(), notification)
    }

    private suspend fun sendUpdateFailure(id: Long, reason: String) {
        val entity = profiles.queryProfileById(id) ?: return

        val notification = NotificationCompat.Builder(this, SERVICE_RESULT_CHANNEL)
            .setContentTitle(getString(R.string.format_update_failure, entity.name, reason))
            .setContentText(reason)
            .setColor(getColor(R.color.colorAccentService))
            .setSmallIcon(R.drawable.ic_notification)
            .setOnlyAlertOnce(true)
            .build()

        NotificationManagerCompat.from(this)
            .notify(RandomUtils.nextInt(), notification)
    }
}