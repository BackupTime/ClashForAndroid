package com.github.kr328.clash.service.clash.module

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

interface Module : CoroutineScope {
    override val coroutineContext: CoroutineContext
        suspend fun get() = Dispatchers.Unconfined

    suspend fun onCreate()
    suspend fun onStart()
    suspend fun onStop()
    suspend fun onDestroy()
}