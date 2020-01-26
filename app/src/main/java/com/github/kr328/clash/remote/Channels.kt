package com.github.kr328.clash.remote

import android.os.RemoteException
import com.github.kr328.clash.core.event.BandwidthEvent
import com.github.kr328.clash.core.event.LogEvent
import com.github.kr328.clash.service.ipc.IStreamCallback
import com.github.kr328.clash.service.ipc.ParcelableContainer
import com.github.kr328.clash.service.ipc.ParcelablePipe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import java.lang.Exception

class LogChannel: Channel<LogEvent> by Channel(Channel.CONFLATED) {
    fun createCallback(): IStreamCallback {
        return object: IStreamCallback.Stub() {
            override fun complete() {
                close()
            }

            override fun completeExceptionally(reason: String?) {
                close(RemoteException(reason))
            }

            override fun send(data: ParcelableContainer?) {
                try {
                    offer(data!!.data!! as LogEvent)
                }
                catch (e: Exception) {
                    throw RemoteException(e.message)
                }
            }
        }
    }
}
