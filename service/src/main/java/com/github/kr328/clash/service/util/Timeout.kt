package com.github.kr328.clash.service.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun CoroutineScope.timeout(timeout: Long): Job {
    return launch {
        delay(timeout)
    }
}