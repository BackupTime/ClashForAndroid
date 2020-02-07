package com.github.kr328.clash.service.util

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel

fun CoroutineScope.ticker(tick: Long, channel: SendChannel<Int>): Job {
    return launch {
        var count = 0

        while (isActive) {
            channel.send(count++)
            delay(tick)
        }
    }
}