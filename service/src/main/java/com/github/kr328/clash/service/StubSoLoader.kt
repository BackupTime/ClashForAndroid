package com.github.kr328.clash.service

import androidx.annotation.Keep

@Keep
object StubSoLoader {
    fun loadSo() {
        System.loadLibrary("clash")
    }
}