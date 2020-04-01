package com.github.kr328.clash.service.clash.module

interface Module {
    suspend fun onCreate()
    suspend fun onStart()
    suspend fun onStop()
    suspend fun onDestroy()
}