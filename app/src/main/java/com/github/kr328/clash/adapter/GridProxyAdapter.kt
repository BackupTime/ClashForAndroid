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
import com.github.kr328.clash.core.utils.Log
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext


class GridProxyAdapter(private val context: ProxiesActivity, spanCount: Int = 2) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), AbstractProxyAdapter {
    private interface RenderInfo {
        val name: String
        val group: String
    }

    private data class ProxyGroupRenderInfo(val info: AbstractProxyAdapter.ProxyGroupInfo) :
        RenderInfo {
        override val name: String
            get() = info.name
        override val group: String
            get() = info.name
    }

    private data class ProxyRenderInfo(val info: AbstractProxyAdapter.ProxyInfo) : RenderInfo {
        override val name: String
            get() = info.name
        override val group: String
            get() = info.group
    }

    private var rootMutex = Mutex()
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
                    is ProxyGroupRenderInfo -> spanCount
                    is ProxyRenderInfo -> 1
                    else -> throw IllegalArgumentException()
                }
            }
        }
    }

    private var root = listOf<AbstractProxyAdapter.ProxyGroupInfo>()
    override var onSelectProxyListener: suspend (String, String) -> Unit = { _, _ -> }

    private class ProxyGroupHeader(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.name)
        val urlTest: View = view.findViewById(R.id.urlTest)
    }

    private class ProxyItem(view: View) : RecyclerView.ViewHolder(view) {
        val root: MaterialCardView = view.findViewById(R.id.root)
        val prefix: TextView = view.findViewById(R.id.prefix)
        val content: TextView = view.findViewById(R.id.content)
        val delay: TextView = view.findViewById(R.id.delay)
    }

    override suspend fun applyChange(newList: List<AbstractProxyAdapter.ProxyGroupInfo>) =
        withContext(Dispatchers.Default) {
            rootMutex.lock()

            val newRenderList = newList
                .flatMap {
                    listOf(ProxyGroupRenderInfo(it)) + it.proxies.map { p -> ProxyRenderInfo(p) }
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
                root = newList
                renderList = newRenderList
                result.dispatchUpdatesTo(this@GridProxyAdapter)
            }

            rootMutex.unlock()
        }

    override suspend fun getGroupPosition(name: String): Int? {
        return withContext(Dispatchers.Default) {
            renderList.mapIndexed { index, p ->
                if (p is ProxyGroupRenderInfo && p.name == name)
                    index
                else
                    -1
            }.singleOrNull { it >= 0 }
        }
    }

    override suspend fun getCurrentGroup(): String {
        val position = layoutManager.findFirstCompletelyVisibleItemPosition()

        if (position < 0)
            return ""

        return renderList[position].group
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
                val current = renderList[position] as ProxyGroupRenderInfo

                holder.name.text = current.info.name
                holder.urlTest.setOnClickListener {

                }
            }
            is ProxyItem -> {
                val current = renderList[position] as ProxyRenderInfo

                holder.prefix.text = current.info.prefix
                holder.content.text = current.info.content


                if (current.info.delay > 0)
                    holder.delay.text = current.info.delay.toString()
                else
                    holder.delay.text = ""

                if (current.info.active) {
                    holder.prefix.setTextColor(Color.WHITE)
                    holder.content.setTextColor(Color.WHITE)
                    holder.delay.setTextColor(Color.WHITE)
                    holder.root.setCardBackgroundColor(context.getColor(R.color.primaryCardColorStarted))
                } else {
                    holder.prefix.setTextColor(colorOnSurface)
                    holder.content.setTextColor(colorOnSurface)
                    holder.delay.setTextColor(colorOnSurface)
                    holder.root.setCardBackgroundColor(colorSurface)
                }

                if (current.info.selectable) {
                    holder.root.setOnClickListener {
                        context.launch {
                            rootMutex.lock()
                            val n = withContext(Dispatchers.Default) {
                                root.map {
                                    if (it.name == current.group) {
                                        it.copy(proxies = it.proxies.map { p ->
                                            p.copy(active = p.name == current.name)
                                        })
                                    } else {
                                        it
                                    }
                                }
                            }
                            rootMutex.unlock()

                            applyChange(n)

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
            is ProxyGroupRenderInfo -> 1
            is ProxyRenderInfo -> 2
            else -> throw IllegalArgumentException()
        }
    }
}