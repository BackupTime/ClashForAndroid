package com.github.kr328.clash.remote

suspend fun <T> withClash(block: suspend ClashClient.() -> T): T {
    val client = Remote.clash.receive()

    return client.block()
}

suspend fun <T> withProfile(block: suspend ProfileClient.() -> T): T {
    val client = Remote.profile.receive()

    return client.block()
}