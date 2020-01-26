package com.github.kr328.clash.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

suspend fun <T>withClash(block: suspend ClashClient.() -> T): T? {
    val client = ClashClient.instance ?: return null

    return client.block()
}

fun CoroutineScope.launchClash(block: suspend ClashClient.() -> Unit) {
    val client = ClashClient.instance ?: return

    launch {
        client.block()
    }
}