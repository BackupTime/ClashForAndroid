package com.github.kr328.clash.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.R
import com.github.kr328.clash.core.event.LogEvent
import com.github.kr328.clash.utils.format
import java.util.*

class LogAdapter(
    private val context: Context,
    private val logs: List<LogEvent>
) : RecyclerView.Adapter<LogAdapter.Holder>() {
    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val level: TextView = view.findViewById(R.id.level)
        private val time: TextView = view.findViewById(R.id.time)
        private val payload: TextView = view.findViewById(R.id.payload)

        fun bind(logEvent: LogEvent) {
            level.text = logEvent.level.toString()
            time.text = Date(logEvent.time).format(itemView.context, includeDate = false)
            payload.text = logEvent.message
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(context).inflate(R.layout.adapter_log, parent, false))
    }

    override fun getItemCount(): Int {
        return logs.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(logs[position])
    }
}