package com.github.kr328.clash.component.time

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex

class Ticker(private val timeMillis: Long) {
    val c: Channel<Int> = Channel()

    private var tickerJob: Job? = null
    private val locker: Mutex = Mutex()

    suspend fun resume() {
        locker.lock()

        if ( tickerJob == null ) {
            start()
        }

        locker.unlock()
    }

    suspend fun pause() {
        locker.lock()

        if ( tickerJob != null ) {
            tickerJob!!.cancel()
        }

        locker.unlock()
    }

    private suspend fun start() {
        withContext(Dispatchers.Unconfined) {
            tickerJob = launch {
                try {
                    var count = 0

                    while (isActive) {
                        c.send(count++)

                        delay(timeMillis)
                    }
                }
                finally {
                    locker.lock()

                    tickerJob = null

                    locker.unlock()
                }
            }
        }
    }
}