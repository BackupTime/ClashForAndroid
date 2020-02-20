package com.github.kr328.clash.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.R
import com.github.kr328.clash.model.LogFile
import com.github.kr328.clash.utils.format
import java.util.*

class LogFileAdapter(
    private val context: Context,
    private val onItemClicked: (LogFile) -> Unit,
    private val onMenuClicked: (LogFile) -> Unit
) : RecyclerView.Adapter<LogFileAdapter.Holder>() {
    var fileList: List<LogFile> = emptyList()

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val root: View = view.findViewById(R.id.root)
        val fileName: TextView = view.findViewById(R.id.fileName)
        val date: TextView = view.findViewById(R.id.date)
        val menu: View = view.findViewById(R.id.menu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(
            LayoutInflater.from(context).inflate(
                R.layout.adapter_log_file,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return fileList.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val current = fileList[position]
        val date = Date(current.date)

        holder.fileName.text = current.fileName
        holder.date.text = date.format(context)
        holder.menu.setOnClickListener {
            onMenuClicked(current)
        }
        holder.root.setOnClickListener {
            onItemClicked(current)
        }
    }
}