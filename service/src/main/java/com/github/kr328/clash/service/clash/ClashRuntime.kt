package com.github.kr328.clash.service.clash

import com.github.kr328.clash.service.clash.module.Module
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ClashRuntime {
    companion object {
        private val GIL = Mutex()
    }

    private val modules: MutableList<Module> = mutableListOf()
    private val mutex = Mutex()
    private var started = false
    private var stopped = false

    suspend fun start() = mutex.withLock {
        if (!GIL.tryLock())
            throw IllegalStateException("ClashRuntime running")

        if (started || stopped)
            return

        started = true

        modules.forEach {
            it.onStart()
        }
    }

    suspend fun stop() = mutex.withLock {
        if (!started || stopped)
            return

        modules.forEach {
            it.onStop()
            it.onDestroy()
        }

        GIL.unlock()
    }

    suspend fun <T : Module> install(module: T, configure: T.() -> Unit = {}) = mutex.withLock {
        if (stopped)
            return

        stopped = true

        modules.add(module)

        module.onCreate()
        module.configure()

        if (started)
            module.onStart()
    }
}