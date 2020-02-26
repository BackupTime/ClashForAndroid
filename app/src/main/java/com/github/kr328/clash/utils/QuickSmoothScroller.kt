package com.github.kr328.clash.utils

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView

class QuickSmoothScroller(context: Context, target: Int) :
    LinearSmoothScroller(context) {
    companion object {
        const val MAX_OFFSET = 2
    }

    var started = {}
    var stopped = {}

    init {
        targetPosition = target
    }

    override fun getVerticalSnapPreference(): Int {
        return SNAP_TO_START
    }

    override fun onStop() {
        super.onStop()

        stopped()
    }

    override fun onStart() {
        super.onStart()

        started()
    }

    override fun onSeekTargetStep(dx: Int, dy: Int, state: RecyclerView.State, action: Action) {
        when (val lm = layoutManager) {
            is LinearLayoutManager -> {
                val current = lm.findFirstCompletelyVisibleItemPosition()

                if (targetPosition > current && targetPosition - current > MAX_OFFSET)
                    action.jumpTo(targetPosition - MAX_OFFSET)
                else if (current > targetPosition && current - targetPosition > MAX_OFFSET)
                    action.jumpTo(targetPosition + MAX_OFFSET)
            }
            is GridLayoutManager -> {
                val current = lm.findFirstCompletelyVisibleItemPosition()

                if (targetPosition > current && targetPosition - current > MAX_OFFSET)
                    action.jumpTo(targetPosition - MAX_OFFSET)
                else if (current > targetPosition && current - targetPosition > MAX_OFFSET)
                    action.jumpTo(targetPosition + MAX_OFFSET)
            }
        }

        super.onSeekTargetStep(dx, dy, state, action)
    }
}