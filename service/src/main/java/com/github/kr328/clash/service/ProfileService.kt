package com.github.kr328.clash.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.content.FileProvider
import com.github.kr328.clash.core.Global
import com.github.kr328.clash.core.utils.Log
import com.github.kr328.clash.service.data.ClashDatabase
import com.github.kr328.clash.service.data.ClashProfileEntity
import com.github.kr328.clash.service.ipc.IStreamCallback
import com.github.kr328.clash.service.ipc.ParcelableContainer
import com.github.kr328.clash.service.transact.ProfileRequest
import com.github.kr328.clash.service.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.util.*

class ProfileService : BaseService() {
    private val service = this
    private val queue: MutableMap<Long, Channel<ProfileRequest>> = Hashtable()
    private val pending = mutableListOf<ProfileRequest>()

    private val profiles = ClashDatabase.getInstance(Global.application).openClashProfileDao()
    private val processor = ProfileProcessor(this)

    override fun onBind(intent: Intent?): IBinder? {
        return object : IProfileService.Stub() {
            override fun enqueueRequest(request: ProfileRequest?) {
                service.enqueueRequest(request ?: return)
            }

            override fun queryActiveProfile(): ClashProfileEntity? {
                return runBlocking {
                    profiles.queryActiveProfile()
                }
            }

            override fun queryProfiles(): Array<ClashProfileEntity> {
                return runBlocking {
                    profiles.queryProfiles()
                }
            }

            override fun setActiveProfile(id: Long) {
                launch {
                    profiles.setActiveProfile(id)

                    broadcastProfileChanged(service)
                }
            }

            override fun requestProfileEditUri(id: Long): String? {
                return runBlocking {
                    val entity = profiles.queryProfileById(id) ?: return@runBlocking null

                    val baseDir = cacheDir.resolve("profiles").apply { mkdirs() }

                    val fileName = RandomUtils.fileName(baseDir, ".yaml")

                    val file = resolveProfile(entity.id).copyTo(baseDir.resolve(fileName))

                    val url = FileProvider.getUriForFile(
                        service,
                        "$packageName${Constants.PROFILE_PROVIDER_SUFFIX}",
                        file
                    ).toString()

                    Log.d("Generated template file $file")

                    "$url?id=${entity.id}&fileName=$fileName"
                }
            }

            override fun commitProfileEditUri(uri: String?) {
                val u = Uri.parse(uri)

                if (u == null || u == Uri.EMPTY)
                    return

                val id = u.getQueryParameter("id")?.toLongOrNull() ?: return
                val fileName = u.getQueryParameter("fileName") ?: return

                val request = ProfileRequest().withId(id).withURL(u)
                    .withCallback(object : IStreamCallback.Stub() {
                        override fun complete() {
                            cacheDir.resolve("profiles/$fileName").delete()
                        }

                        override fun completeExceptionally(reason: String?) {
                            cacheDir.resolve("profiles/$fileName").delete()
                        }

                        override fun send(data: ParcelableContainer?) {

                        }
                    })
                val i = ProfileBackgroundService::class.intent
                    .putExtra(Intents.INTENT_EXTRA_PROFILE_REQUEST, request)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    startForegroundService(i)
                else
                    startService(i)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        Log.d("ProfileService.onCreate")
    }

    override fun onDestroy() {
        super.onDestroy()

        pending.forEach {
            it.callback?.completeExceptionally("Canceled")
        }

        Log.d("ProfileService.onDestroy")
    }

    private fun createChannelForRequests(id: Long): Channel<ProfileRequest> {
        return Channel<ProfileRequest>(Channel.UNLIMITED).also {
            launch {
                try {
                    Log.d("Coroutine for $id launched")

                    while (isActive) {
                        val request = withTimeout(1000 * 30) {
                            it.receive()
                        }

                        Log.d("Handling $id")
                        handleRequest(request)
                    }
                } finally {
                    Log.d("Coroutine for $id exited")

                    queue.remove(id)
                }
            }
        }
    }

    private fun enqueueRequest(request: ProfileRequest) {
        Log.d("Request $request enqueue")

        pending.add(request)

        queue.computeIfAbsent(request.id) {
            createChannelForRequests(it)
        }.offer(request)
    }

    private suspend fun handleRequest(request: ProfileRequest) {
        try {
            request.callback?.send(null)

            when (request.action) {
                ProfileRequest.Action.UPDATE_OR_CREATE ->
                    handleUpdateOrCreate(request)
                ProfileRequest.Action.REMOVE ->
                    removeProfile(request)
                ProfileRequest.Action.CLEAR ->
                    clearProfile(request)
            }

            request.callback?.complete()

            broadcastProfileChanged(this)
        } catch (e: Exception) {
            Log.w("handleRequest", e)
            request.callback?.completeExceptionally(e.message)
        } finally {
            pending.remove(request)
        }
    }

    private suspend fun handleUpdateOrCreate(request: ProfileRequest) =
        withContext(Dispatchers.IO) {
            val id = request.id

            val entity: ClashProfileEntity =
                if (id == 0L) {
                    ClashProfileEntity(
                        requireNotNull(request.name),
                        requireNotNull(request.type),
                        requireNotNull(request.url).toString(),
                        request.source?.toString(),
                        false,
                        0,
                        request.interval.takeIf { it >= 0 } ?: 0,
                        profiles.generateNewId()
                    )
                } else {
                    val e = profiles.queryProfileById(id) ?: return@withContext

                    e.copy(
                        name = request.name ?: e.name,
                        uri = request.url?.toString() ?: e.uri,
                        updateInterval = request.interval.takeIf { it >= 0 } ?: e.updateInterval
                    )
                }

            processor.createOrUpdate(entity, id == 0L)

            if (entity.updateInterval > 0) {
                val nextRequest =
                    ProfileRequest().action(ProfileRequest.Action.UPDATE_OR_CREATE).withId(entity.id)

                requireNotNull(getSystemService(AlarmManager::class.java)).set(
                    AlarmManager.RTC,
                    entity.lastUpdate + entity.updateInterval,
                    PendingIntent.getBroadcast(
                        service,
                        RandomUtils.nextInt(),
                        Intent(Intents.INTENT_ACTION_PROFILE_ENQUEUE_REQUEST)
                            .setComponent(ProfileRequestReceiver::class.componentName)
                            .putExtra(Intents.INTENT_EXTRA_PROFILE_REQUEST, nextRequest),
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
            }
        }

    private suspend fun removeProfile(request: ProfileRequest) = withContext(Dispatchers.IO) {
        processor.remove(request.id)
    }

    private suspend fun clearProfile(request: ProfileRequest) = withContext(Dispatchers.IO) {
        processor.clear(request.id)
    }
}