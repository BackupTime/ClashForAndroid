package com.github.kr328.clash.view

import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.ProxiesActivity
import com.github.kr328.clash.adapter.AbstractProxyAdapter
import com.github.kr328.clash.adapter.ProxyChipAdapter
import kotlinx.coroutines.channels.Channel

class ProxiesTabMediator(
    private val context: ProxiesActivity,
    private val proxiesView: RecyclerView,
    private val chipView: RecyclerView
) {
    private var preventChipScroll = false
    private val updateChipChannel = Channel<Unit>(Channel.CONFLATED)

    init {
        proxiesView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateChipChannel.offer(Unit)
            }
        })
    }

    suspend fun exec() {
        while (true) {
            var currentChecked = ""

            while (true) {
                updateChipChannel.receive()

                val currentGroup = (proxiesView.adapter!! as AbstractProxyAdapter).getCurrentGroup()

                if (currentChecked == currentGroup)
                    continue

                currentChecked = currentGroup

                (chipView.adapter!! as ProxyChipAdapter).apply {
                    selected = currentChecked

                    if (!preventChipScroll)
                        chipView.smoothScrollToPosition(chips.indexOf(currentChecked))
                }
            }
        }
    }

    suspend fun scrollTo(group: String) {
        (proxiesView.adapter!! as AbstractProxyAdapter).apply {
            val index = getGroupPosition(group) ?: return

            scrollTo(index)
        }
    }

    private fun scrollTo(position: Int) {
        val scroller = (object : LinearSmoothScroller(context) {
            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_START
            }

            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
                return 35f / displayMetrics!!.densityDpi
            }

            override fun onStop() {
                super.onStop()

                preventChipScroll = false
            }

            override fun onStart() {
                super.onStart()

                preventChipScroll = true
            }

            init {
                targetPosition = position
            }
        })

        proxiesView.layoutManager!!.startSmoothScroll(scroller)
    }
}