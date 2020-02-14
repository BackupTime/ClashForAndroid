package com.github.kr328.clash.adapter

import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.ProxiesActivity
import com.github.kr328.clash.R
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxyGroup
import com.github.kr328.clash.core.utils.Log
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class GridProxyAdapter(private val context: ProxiesActivity, spanCount: Int = 2) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), AbstractProxyAdapter {
    private interface RenderInfo {
        val name: String
    }

    private data class ProxyGroupInfo(override val name: String) : RenderInfo
    private data class ProxyInfo(
        override val name: String,
        val type: Proxy.Type,
        val group: String,
        val selectable: Boolean,
        val delay: Short,
        val active: Boolean
    ) : RenderInfo

    private var renderList = emptyList<RenderInfo>()
    @ColorInt
    private val colorSurface: Int
    @ColorInt
    private val colorOnSurface: Int

    init {
        val typedValue = TypedValue()

        context.theme.resolveAttribute(R.attr.colorSurface, typedValue, true)
        colorSurface = typedValue.data

        context.theme.resolveAttribute(R.attr.colorOnSurface, typedValue, true)
        colorOnSurface = typedValue.data
    }

    val layoutManager = GridLayoutManager(context, spanCount).apply {
        spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (renderList[position]) {
                    is ProxyGroupInfo -> spanCount
                    is ProxyInfo -> 1
                    else -> throw IllegalArgumentException()
                }
            }
        }
    }

    override var root = listOf<ProxyGroup>()
    override var onSelectProxyListener: suspend (String, String) -> Unit = { _, _ -> }

    private class ProxyGroupHeader(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.name)
        val urlTest: View = view.findViewById(R.id.urlTest)
    }

    private class ProxyItem(view: View) : RecyclerView.ViewHolder(view) {
        val root: MaterialCardView = view.findViewById(R.id.root)
        val name: TextView = view.findViewById(R.id.name)
        val type: TextView = view.findViewById(R.id.type)
        val delay: TextView = view.findViewById(R.id.delay)
    }

    override suspend fun applyChange() = withContext(Dispatchers.Default) {
        val newRenderList = root
            .flatMap {
                listOf(ProxyGroupInfo(it.name)) + it.proxies.map { p ->
                    ProxyInfo(
                        p.name,
                        p.type,
                        it.name,
                        it.type == Proxy.Type.SELECT,
                        p.delay.toShort(),
                        it.current == p.name
                    )
                }
            }

        val oldRenderList = renderList

        val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldRenderList[oldItemPosition]::class == newRenderList[newItemPosition]::class &&
                        oldRenderList[oldItemPosition].name == newRenderList[newItemPosition].name

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldRenderList[oldItemPosition] == newRenderList[newItemPosition]

            override fun getOldListSize(): Int = oldRenderList.size
            override fun getNewListSize(): Int = newRenderList.size
        })

        withContext(Dispatchers.Main) {
            renderList = newRenderList
            result.dispatchUpdatesTo(this@GridProxyAdapter)
        }
    }

    override suspend fun scrollToGroup(name: String) {
        val position = withContext(Dispatchers.Default) {
            renderList.mapIndexed { index, p ->
                if ( p is ProxyGroupInfo && p.name == name )
                    index
                else
                    -1
            }.singleOrNull { it >= 0 }
        } ?: return

        layoutManager.scrollToPositionWithOffset(position, 0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(context)

        return when (viewType) {
            1 -> ProxyGroupHeader(
                layoutInflater
                    .inflate(R.layout.adapter_grid_proxy_group, parent, false)
            )
            2 -> ProxyItem(
                layoutInflater
                    .inflate(R.layout.adapter_grid_proxy, parent, false)
            )
            else -> throw IllegalArgumentException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ProxyGroupHeader -> {
                val current = renderList[position] as ProxyGroupInfo

                holder.name.text = current.name
                holder.urlTest.setOnClickListener {

                }
            }
            is ProxyItem -> {
                val current = renderList[position] as ProxyInfo

                holder.name.text = current.name
                holder.type.text = current.type.toString()


                if (current.delay > 0)
                    holder.delay.text = current.delay.toString()
                else
                    holder.delay.text = "N/A"

                if (current.active) {
                    holder.name.setTextColor(Color.WHITE)
                    holder.type.setTextColor(Color.WHITE)
                    holder.delay.setTextColor(Color.WHITE)
                    holder.root.setCardBackgroundColor(context.getColor(R.color.primaryCardColorStarted))
                } else {
                    holder.name.setTextColor(colorOnSurface)
                    holder.type.setTextColor(colorOnSurface)
                    holder.delay.setTextColor(colorOnSurface)
                    holder.root.setCardBackgroundColor(colorSurface)
                }

                if (current.selectable) {
                    holder.root.setOnClickListener {
                        root = root.map {
                            if (it.name == current.group) {
                                it.copy(current = current.name)
                            } else {
                                it
                            }
                        }

                        context.launch {
                            applyChange()

                            onSelectProxyListener(current.group, current.name)
                        }
                    }
                    holder.root.isClickable = true
                    holder.root.isFocusable = true
                } else {
                    holder.root.setOnClickListener(null)
                    holder.root.isClickable = false
                    holder.root.isFocusable = false
                }
            }
        }
    }

    override fun getItemCount(): Int = renderList.size
    override fun getItemViewType(position: Int): Int {
        return when (renderList[position]) {
            is ProxyGroupInfo -> 1
            is ProxyInfo -> 2
            else -> throw IllegalArgumentException()
        }
    }
}