package com.github.kr328.clash.service.util

import kotlinx.coroutines.*

fun CoroutineScope.timeout(timeout: Long): Job {
    return launch {
        delay(timeout)
    }
}