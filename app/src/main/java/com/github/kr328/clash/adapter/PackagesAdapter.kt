package com.github.kr328.clash.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.R
import com.google.android.material.checkbox.MaterialCheckBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.streams.toList

class PackagesAdapter(
    private val context: Context,
    private val apps: List<AppInfo>
) :
    RecyclerView.Adapter<PackagesAdapter.Holder>() {
    data class AppInfo(
        val packageName: String, val label: String, val icon: Drawable,
        val installTime: Long, val updateTime: Long, val isSystem: Boolean
    )

    enum class Sort {
        NAME, PACKAGE, INSTALL_TIME, UPDATE_TIME
    }

    val selectedPackages: MutableSet<String> = mutableSetOf()

    private var filteredCache: List<AppInfo> = apps

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val root: View = view.findViewById(R.id.root)
        val icon: ImageView = view.findViewById(R.id.icon)
        val label: TextView = view.findViewById(R.id.label)
        val packageName: TextView = view.findViewById(R.id.packageName)
        val checkbox: MaterialCheckBox = view.findViewById(R.id.checkbox)
    }

    suspend fun applyFilter(keyword: String, sort: Sort, decrease: Boolean, systemApp: Boolean) {
        withContext(Dispatchers.Default) {
            val newList = apps.parallelStream()
                .filter {
                    (it.label.contains(keyword, true)
                            || it.packageName.contains(keyword, true))
                            && (systemApp || !it.isSystem)
                }
                .sorted { a, b ->
                    val sA = selectedPackages.contains(a.packageName)
                    val sB = selectedPackages.contains(b.packageName)

                    if (sA != sB) {
                        when {
                            sA -> return@sorted -1
                            sB -> return@sorted 1
                        }
                    }

                    val result = when (sort) {
                        Sort.NAME -> a.label.compareTo(b.label, true)
                        Sort.PACKAGE -> a.packageName.compareTo(b.packageName)
                        Sort.INSTALL_TIME -> (a.installTime - b.installTime).toInt()
                        Sort.UPDATE_TIME -> (a.updateTime - b.updateTime).toInt()
                    }

                    if (decrease) -result else result
                }
                .toList()
            val oldList = filteredCache

            val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldList[oldItemPosition].packageName == newList[newItemPosition].packageName
                }

                override fun getOldListSize(): Int {
                    return oldList.size
                }

                override fun getNewListSize(): Int {
                    return newList.size
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    return areItemsTheSame(oldItemPosition, newItemPosition) &&
                            (selectedPackages.contains(oldList[oldItemPosition].packageName) ==
                                    selectedPackages.contains(newList[newItemPosition].packageName))
                }

            })

            withContext(Dispatchers.Main) {
                filteredCache = newList
                result.dispatchUpdatesTo(this@PackagesAdapter)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(context).inflate(R.layout.adapter_package, parent, false))
    }

    override fun getItemCount(): Int {
        return filteredCache.size
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val current = filteredCache[position]

        holder.icon.setImageDrawable(current.icon)
        holder.label.text = current.label
        holder.packageName.text = current.packageName
        holder.checkbox.isChecked = selectedPackages.contains(current.packageName)
        holder.root.setOnClickListener {
            if (selectedPackages.contains(current.packageName)) {
                selectedPackages.remove(current.packageName)
            } else {
                selectedPackages.add(current.packageName)
            }

            notifyItemChanged(filteredCache.indexOf(current))
        }
    }
}