package com.github.kr328.clash.remote

suspend fun <T> withClash(block: suspend ClashClient.() -> T): T? {
    val client = ClashClient.clashInstanceChannel.receive()

    return client.block()
}