package com.github.kr328.clash.service.util

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

val DefaultThreadPool: ExecutorService = Executors.newCachedThreadPool()