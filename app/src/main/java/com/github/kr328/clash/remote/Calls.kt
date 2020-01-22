package com.github.kr328.clash.remote

suspend fun withClash(block: suspend (ClashClient) -> Unit) {
    val client = ClashClient.instance ?: return

    block(client)
}