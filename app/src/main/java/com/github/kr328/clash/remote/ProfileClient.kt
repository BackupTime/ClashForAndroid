package com.github.kr328.clash.remote

import android.os.RemoteException
import com.github.kr328.clash.service.IProfileService
import com.github.kr328.clash.service.transact.IStreamCallback
import com.github.kr328.clash.service.transact.ParcelableContainer
import com.github.kr328.clash.service.model.ProfileMetadata
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileClient(private val service: IProfileService) {
    suspend fun acquireUnused(type: ProfileMetadata.Type) = withContext(Dispatchers.IO) {
        service.acquireUnused(type.name)
    }

    suspend fun acquireCloned(id: Long) = withContext(Dispatchers.IO) {
        service.acquireCloned(id)
    }

    suspend fun updateMetadata(id: Long, metadata: ProfileMetadata) = withContext(Dispatchers.IO) {
        service.updateMetadata(id, metadata)
    }

    suspend fun commitAsync(id: Long) = withContext(Dispatchers.IO) {
        CompletableDeferred<Unit>().apply {
            service.commit(id, object : IStreamCallback.Stub() {
                override fun complete() {
                    complete(Unit)
                }

                override fun completeExceptionally(reason: String?) {
                    completeExceptionally(RemoteException(reason))
                }

                override fun send(data: ParcelableContainer?) {}
            })
        }
    }

    suspend fun cancel(id: Long) = withContext(Dispatchers.IO) {
        service.cancel(id)
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        service.delete(id)
    }

    suspend fun clear(id: Long) = withContext(Dispatchers.IO) {
        service.clear(id)
    }

    suspend fun queryAll(): Array<ProfileMetadata> = withContext(Dispatchers.IO) {
        service.queryAll()
    }

    suspend fun queryActive(): ProfileMetadata? = withContext(Dispatchers.IO) {
        service.queryActive()
    }

    suspend fun queryById(id: Long): ProfileMetadata? = withContext(Dispatchers.IO) {
        service.queryById(id)
    }

    suspend fun setActive(id: Long) = withContext(Dispatchers.IO) {
        service.setActive(id)
    }
}