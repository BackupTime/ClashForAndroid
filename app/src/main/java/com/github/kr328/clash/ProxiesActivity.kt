package com.github.kr328.clash

import android.os.Bundle
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.adapter.AbstractProxyAdapter
import com.github.kr328.clash.adapter.GridProxyAdapter
import com.github.kr328.clash.adapter.ProxyChipAdapter
import com.github.kr328.clash.remote.withClash
import com.github.kr328.clash.utils.ProxySorter
import kotlinx.android.synthetic.main.activity_proxies.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProxiesActivity : BaseActivity() {
    private val activity: ProxiesActivity
        get() = this
    private var maskedGroup: MutableSet<String> = mutableSetOf()
    private val updateChipChannel = Channel<Unit>(Channel.CONFLATED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proxies)
        setSupportActionBar(toolbar)

        GridProxyAdapter(this).apply {
            mainList.layoutManager = layoutManager
            mainList.adapter = this

            onSelectProxyListener = { group, name ->
                withClash {
                    setSelectProxy(group, name)
                }
            }
        }

        mainList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateChipChannel.offer(Unit)
            }
        })

        chipList.adapter = ProxyChipAdapter(this) {
            launch {
                (mainList.adapter!! as AbstractProxyAdapter).getGroupPosition(it)?.also {
                    val scroller = (object : LinearSmoothScroller(activity) {
                        override fun getVerticalSnapPreference(): Int {
                            return SNAP_TO_START
                        }

                        override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics?): Float {
                            return 40f / displayMetrics!!.densityDpi
                        }

                        init {
                            targetPosition = it
                        }
                    })

                    mainList.layoutManager?.startSmoothScroll(scroller)
                }
            }
        }
        chipList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        chipList.itemAnimator?.changeDuration = 80

        launch {
            var currentChecked = ""

            while (isActive) {
                updateChipChannel.receive()

                val currentGroup = (mainList.adapter!! as AbstractProxyAdapter).getCurrentGroup()

                if (currentChecked == currentGroup)
                    continue

                currentChecked = currentGroup

                (chipList.adapter!! as ProxyChipAdapter).apply {
                    selected = currentChecked

                    (chipList.layoutManager!! as LinearLayoutManager)
                        .scrollToPositionWithOffset(chips.indexOf(currentChecked), 0)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()

        refreshList()
    }

    override suspend fun onClashStarted() {
        finish()
    }

    private fun refreshList(scrollTo: String? = null) {
        launch {
            val proxies = withClash {
                queryAllProxyGroups()
            }

            val sorter = ProxySorter(ProxySorter.Order.DEFAULT, ProxySorter.Order.DEFAULT)

            val filtered = withContext(Dispatchers.Default) {
                sorter.sort(proxies.toList())
            }

            (mainList.adapter!! as AbstractProxyAdapter).apply {
                root = filtered.filter { !maskedGroup.contains(it.name) }

                applyChange()
            }

            (chipList.adapter!! as ProxyChipAdapter).chips = filtered.map { it.name }
        }
    }
}