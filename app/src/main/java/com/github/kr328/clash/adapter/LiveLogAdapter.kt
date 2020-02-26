package com.github.kr328.clash.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.collection.CircularArray
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.R
import com.github.kr328.clash.core.event.LogEvent

class LiveLogAdapter(private val context: Context) : RecyclerView.Adapter<LogAdapter.Holder>() {
    companion object {
        const val MAX_LOG_ITEMS = 100
    }

    private val circularArray = CircularArray<LogEvent>(MAX_LOG_ITEMS)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogAdapter.Holder {
        return LogAdapter.Holder(
            LayoutInflater.from(context).inflate(
                R.layout.adapter_log,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return circularArray.size()
    }

    override fun onBindViewHolder(holder: LogAdapter.Holder, position: Int) {
        holder.bind(circularArray[position])
    }

    fun insertItems(i: List<LogEvent>) {
        val items = if (i.size > MAX_LOG_ITEMS) {
            i.subList(i.size - MAX_LOG_ITEMS, i.size)
        } else i

        val predictSize = items.size + circularArray.size()

        if (predictSize > MAX_LOG_ITEMS) {
            val removeSize = predictSize - MAX_LOG_ITEMS
            notifyItemRangeRemoved(MAX_LOG_ITEMS - removeSize, removeSize)
            circularArray.removeFromEnd(removeSize)
        }

        items.forEach {
            circularArray.addFirst(it)
        }

        notifyItemRangeInserted(0, items.size)
    }
}