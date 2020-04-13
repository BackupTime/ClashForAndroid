package com.github.kr328.clash.service

import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.os.RemoteException
import com.github.kr328.clash.service.data.ProfileDao
import com.github.kr328.clash.service.ipc.IStreamCallback
import com.github.kr328.clash.service.model.ProfileMetadata
import com.github.kr328.clash.service.model.toProfileMetadata
import com.github.kr328.clash.service.util.broadcastProfileChanged
import com.github.kr328.clash.service.util.resolveBaseDir
import com.github.kr328.clash.service.util.resolveProfileFile
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*

class ProfileService : BaseService() {
    private val service = this
    private val lock = Mutex()
    private val pending = mutableMapOf<Long, ProfileMetadata>()
    private val tasks = mutableMapOf<Long, IStreamCallback?>()
    private val request = Channel<Unit>(Channel.CONFLATED)

    override fun onBind(intent: Intent?): IBinder? {
        return object : IProfileService.Stub() {
            override fun setActive(id: Long) {
                launch {
                    ProfileDao.setActive(id)

                    service.broadcastProfileChanged()
                }
            }

            override fun commit(id: Long, callback: IStreamCallback?) {
                launch {
                    lock.withLock {
                        tasks[id] = callback

                        request.offer(Unit)
                    }
                }
            }

            override fun cancel(id: Long) {
                launch {
                    lock.withLock {
                        if (pending.remove(id) != null)
                            service.resolveBaseDir(id).deleteRecursively()
                    }
                }
            }

            override fun acquireUnused(type: String): Long {
                return runBlocking {
                    lock.withLock {
                        val id = (ProfileDao.queryAllIds() + pending.keys).max()?.plus(1) ?: 0

                        pending[id] = ProfileMetadata(
                            id = id,
                            name = "",
                            type = ProfileMetadata.Type.valueOf(type),
                            uri = Uri.EMPTY,
                            source = null,
                            active = false,
                            interval = 0,
                            lastModified = 0
                        )

                        service.resolveBaseDir(id).apply {
                            deleteRecursively()
                            mkdirs()
                        }

                        id
                    }
                }
            }

            override fun queryActive(): ProfileMetadata? {
                return runBlocking {
                    ProfileDao.queryActive()?.toProfileMetadata(service)
                }
            }

            override fun delete(id: Long) {
                launch {
                    lock.withLock {
                        pending.remove(id)
                        ProfileDao.remove(id)
                    }

                    service.resolveProfileFile(id).delete()
                    service.resolveBaseDir(id).deleteRecursively()

                    service.broadcastProfileChanged()
                }
            }

            override fun clear(id: Long) {
                launch {
                    withContext(Dispatchers.IO) {
                        resolveBaseDir(id).listFiles()?.forEach {
                            it.deleteRecursively()
                        }
                    }
                }
            }

            override fun queryAll(): Array<ProfileMetadata> {
                return runBlocking {
                    ProfileDao.queryAll().map { it.toProfileMetadata(service) }.toTypedArray()
                }
            }

            override fun queryById(id: Long): ProfileMetadata? {
                return runBlocking {
                    lock.withLock {
                        queryMetadataById(id)
                    }
                }
            }

            override fun updateMetadata(id: Long, metadata: ProfileMetadata?) {
                launch {
                    lock.withLock {
                        pending[id] = metadata ?: return@launch
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        launch {
            process()
        }
    }

    private suspend fun process() {
        while (isActive) {
            request.receive()

            val ctx = lock.withLock {
                tasks.entries.firstOrNull()?.also {
                    tasks.remove(it.key)
                }
            } ?: continue

            try {
                val metadata = queryMetadataById(ctx.key)
                    ?: throw RemoteException("No such profile")

                lock.withLock {
                    pending.remove(metadata.id)
                }

                ProfileProcessor.createOrUpdate(service, metadata)

                ctx.value?.complete()
            } catch (e: Exception) {
                ctx.value?.completeExceptionally(e.message)
            }

            request.offer(Unit)
        }
    }

    private suspend fun queryMetadataById(id: Long): ProfileMetadata? {
        return pending[id] ?: ProfileDao.queryById(id)?.toProfileMetadata(service)
    }
}