package com.github.kr328.clash.service.util

import android.os.Process

object UserUtils {
    private const val PER_USER_RANGE: Int = 100000

    val currentUserId: Int
        get() = Process.myUid() / PER_USER_RANGE
}