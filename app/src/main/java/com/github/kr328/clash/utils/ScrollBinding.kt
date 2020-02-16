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
        suspend fun getCurrentMasterToken(): String
        suspend fun onMasterTokenChanged(token: String)
        suspend fun getMasterTokenPosition(token: String): Int
        suspend fun doMasterScroll(scroller: LinearSmoothScroller)
    }

    private val updateChannel = Channel<Unit>(Channel.CONFLATED)
    private var preventSlaveScroll = false

    fun sendMasterScrolled() {
        updateChannel.offer(Unit)
    }

    suspend fun scrollMaster(token: String) {
        val position = callback.getMasterTokenPosition(token)

        if (position < 0)
            return

        val scroller = (object : LinearSmoothScroller(context) {
            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_START
            }

            override fun onStop() {
                super.onStop()

                preventSlaveScroll = false
            }

            override fun onStart() {
                super.onStart()

                preventSlaveScroll = true
            }

            init {
                targetPosition = position
            }
        })

        callback.doMasterScroll(scroller)
    }

    suspend fun exec() {
        var lastToken: String? = null

        while (true) {
            updateChannel.receive()

            val currentToken = callback.getCurrentMasterToken()
            if (lastToken == currentToken)
                continue

            lastToken = currentToken

            callback.onMasterTokenChanged(currentToken)

            delay(200)
        }
    }
}