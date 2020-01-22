package com.github.kr328.clash.remote

import com.github.kr328.clash.core.event.BandwidthEvent
import com.github.kr328.clash.core.event.LogEvent
import com.github.kr328.clash.service.ipc.ParcelablePipe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel

class LogChannel(private val pipe: ParcelablePipe): Channel<LogEvent> by Channel(Channel.CONFLATED) {
    init {
        pipe.onReceive {
            offer(it as LogEvent)
        }
    }

    override fun cancel(cause: CancellationException?) {
        pipe.close()
        close(cause)
    }
}

class BandwidthChannel(private val pipe: ParcelablePipe): Channel<BandwidthEvent> by Channel(Channel.CONFLATED) {
    init {
        pipe.onReceive {
            offer(it as BandwidthEvent)
        }
    }

    override fun cancel(cause: CancellationException?) {
        pipe.close()
        close(cause)
    }
}