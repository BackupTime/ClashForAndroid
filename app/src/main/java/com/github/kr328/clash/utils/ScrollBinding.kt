package com.github.kr328.clash.utils

import android.content.Context
import androidx.recyclerview.widget.LinearSmoothScroller
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay

class ScrollBinding(
    private val context: Context,
    private val callback: Callback
) {
    interface Callback {
        fun getCurrentMasterToken(): String
        fun onMasterTokenChanged(token: String)
        fun getMasterTokenPosition(token: String): Int
        fun doMasterScroll(scroller: LinearSmoothScroller, target: Int)
    }

    private val updateChannel = Channel<Unit>(Channel.CONFLATED)
    private var preventSlaveScroll = false

    fun sendMasterScrolled() {
        updateChannel.offer(Unit)
    }

    fun scrollMaster(token: String) {
        val position = callback.getMasterTokenPosition(token)

        if (position < 0)
            return

        val scroller = QuickSmoothScroller(context, position)

        callback.doMasterScroll(scroller, position)
    }

    suspend fun exec() {
        var lastToken: String? = null

        while (true) {
            updateChannel.receive()

            val currentToken = callback.getCurrentMasterToken()
            if (preventSlaveScroll || lastToken == currentToken)
                continue

            lastToken = currentToken

            callback.onMasterTokenChanged(currentToken)

            delay(200)
        }
    }
}