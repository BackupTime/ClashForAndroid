package com.github.kr328.clash.service

import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.service.data.ClashDatabase
import com.github.kr328.clash.service.data.ClashProfileDao
import com.github.kr328.clash.service.data.ClashProfileEntity
import com.github.kr328.clash.service.transact.ProfileRequest
import com.github.kr328.clash.service.util.*
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class ClashProfileService : Service() {
    companion object {
        private const val SERVICE_STATUS_CHANNEL = "profile_service_status"
        private const val SERVICE_RESULT_CHANNEL = "profile_service_result"
        private const val SERVICE_NOTIFICATION_ID = 10000
    }

    private val requestQueue = LinkedBlockingQueue<ProfileRequest>()

    val service: ClashProfileService
        get() = this
    val profiles: ClashProfileDao by lazy {
        ClashDatabase.getInstance(service).openClashProfileDao()
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannels()

        updateNotificationWaiting()

        thread {
            while (true) {
                val request = try {
                    updateNotificationWaiting()
                    requestQueue.poll(60, TimeUnit.SECONDS) ?: break
                } catch (e: InterruptedException) {
                    break
                }

                handleRequest(request)
            }

            stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return Binder()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            Intents.INTENT_ACTION_PROFILE_SETUP -> {
                resetProfileUpdateAlarm()
            }
            Intents.INTENT_ACTION_PROFILE_ENQUEUE_REQUEST -> {
                val request =
                    intent.getParcelableExtra<ProfileRequest>(Intents.INTENT_EXTRA_PROFILE_REQUEST)
                if (request != null)
                    enqueueRequest(request)
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopForeground(true)

        super.onDestroy()
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

    private fun enqueueRequest(request: ProfileRequest) {
        requestQueue.offer(request)
    }

    private fun handleRequest(request: ProfileRequest) {
        when (request.action) {
            ProfileRequest.Action.UPDATE_OR_CREATE ->
                handleUpdateOrCreate(request)
            ProfileRequest.Action.REMOVE ->
                removeProfile(request)
        }

        sendBroadcastSelf(
            Intent(Intents.INTENT_ACTION_PROFILE_CHANGED)
                .putExtra(
                    Intents.INTENT_EXTRA_PROFILE_ACTIVE,
                    profiles.queryActiveProfile()
                )
        )
    }

    private fun handleUpdateOrCreate(request: ProfileRequest) {
        val id = request.id ?: 0

        var entity: ClashProfileEntity?

        try {
            if (id == 0L) {
                entity = ClashProfileEntity(
                    requireNotNull(request.name),
                    requireNotNull(request.type),
                    requireNotNull(request.url),
                    RandomUtils.fileName(profileDir, ".yaml"),
                    RandomUtils.fileName(clashDir),
                    false,
                    request.interval ?: 0,
                    0
                )
            } else {
                entity =
                    profiles.queryProfileById(id) ?: throw NullPointerException("Profile not found")

                if (request.name != null)
                    entity = entity.copy(name = requireNotNull(request.name))

                if (request.url != null)
                    entity = entity.copy(uri = requireNotNull(request.url))

                if (request.interval != null)
                    entity = entity.copy(updateInterval = requireNotNull(request.interval))
            }
        } catch (e: Exception) {
            notifyResultNotification(
                false,
                getString(R.string.profile_update_failure, "ID:$id")
            )
            return
        }

        updateNotificationUpdating(entity.name)

        try {
            val url = Uri.parse(entity.uri)

            if (url == null || url == Uri.EMPTY)
                throw IllegalArgumentException("Invalid url $url")

            downloadProfile(url, profileDir.resolve(entity.file), clashDir.resolve(entity.base))

            entity = entity.copy(lastUpdate = System.currentTimeMillis())

            val newId = if (entity.id == 0L)
                profiles.getId(profiles.addProfile(entity))
            else
                profiles.updateProfile(entity).run { entity.id }

            if (entity.updateInterval > 0) {
                val nextRequest =
                    ProfileRequest().action(ProfileRequest.Action.UPDATE_OR_CREATE).withId(newId)

                requireNotNull(getSystemService(AlarmManager::class.java)).set(
                    AlarmManager.RTC,
                    entity.lastUpdate + entity.updateInterval,
                    PendingIntent.getBroadcast(
                        this,
                        RandomUtils.nextInt(),
                        Intent(Intents.INTENT_ACTION_PROFILE_ENQUEUE_REQUEST)
                            .setComponent(ClashProfileReceiver::class.componentName)
                            .putExtra(Intents.INTENT_EXTRA_PROFILE_REQUEST, nextRequest),
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
            }

            notifyResultNotification(
                true,
                getString(R.string.profile_update_completed, entity.name)
            )
        } catch (e: Exception) {
            notifyResultNotification(
                false,
                getString(R.string.profile_update_failure, entity.name)
            )
            throw e
        }
    }

    private fun removeProfile(request: ProfileRequest) {
        val entity = profiles.queryProfileById(request.id ?: return) ?: return

        clashDir.resolve(entity.base).deleteRecursively()
        profileDir.resolve(entity.file).delete()

        profiles.removeProfile(entity.id)

        notifyResultNotification(true, getString(R.string.profile_deleted, entity.name))
    }

    private fun resetProfileUpdateAlarm() {
        DefaultThreadPool.submit {
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
                            .setComponent(ClashProfileReceiver::class.componentName)
                            .putExtra(Intents.INTENT_EXTRA_PROFILE_REQUEST, nextRequest),
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
            }
        }
    }

    private fun downloadProfile(source: Uri, target: File, baseDir: File) {
        if (source.scheme == "content" || source.scheme == "file") {
            val fd = contentResolver.openFileDescriptor(source, "r")
                ?: throw FileNotFoundException("Unable to open file $source")

            Clash.copyProfile(fd.fd, target, baseDir)
        } else {
            Clash.downloadProfile(source.toString(), target, baseDir)
        }
    }
}