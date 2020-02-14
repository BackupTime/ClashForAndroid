package com.github.kr328.clash.remote

import android.os.RemoteException
import com.github.kr328.clash.core.model.General
import com.github.kr328.clash.core.model.ProxyGroup
import com.github.kr328.clash.service.IClashManager
import com.github.kr328.clash.service.ipc.IStreamCallback
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClashClient(private val service: IClashManager) {
    suspend fun setSelectProxy(name: String, proxy: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext service.setSelectProxy(name, proxy)
    }

    suspend fun startHealthCheck(group: String) = withContext(Dispatchers.IO) {
        CompletableDeferred<Unit>().apply {
            service.startHealthCheck(group, object : IStreamCallback.Default() {
                override fun complete() {
                    this@apply.complete(Unit)
                }

                override fun completeExceptionally(reason: String?) {
                    this@apply.completeExceptionally(RemoteException(reason))
                }
            })
        }
    }.await()

    suspend fun queryAllProxyGroups(): List<ProxyGroup> = withContext(Dispatchers.IO) {
        service.queryAllProxies().list
    }

    suspend fun queryGeneral(): General = withContext(Dispatchers.IO) {
        service.queryGeneral()
    }

    suspend fun queryBandwidth(): Long =
        withContext(Dispatchers.IO) {
            service.queryBandwidth()
        }
}