package com.github.kr328.clash

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.adapter.AbstractProxyAdapter
import com.github.kr328.clash.adapter.GridProxyAdapter
import com.github.kr328.clash.adapter.ProxyChipAdapter
import com.github.kr328.clash.remote.withClash
import com.github.kr328.clash.utils.ProxySorter
import com.github.kr328.clash.view.ProxiesTabMediator
import kotlinx.android.synthetic.main.activity_proxies.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProxiesActivity : BaseActivity() {
    private val activity: ProxiesActivity
        get() = this

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proxies)
        setSupportActionBar(toolbar)

        val mediator = ProxiesTabMediator(this, mainList, chipList)

        GridProxyAdapter(this).apply {
            mainList.layoutManager = layoutManager
            mainList.adapter = this

            onSelectProxyListener = { group, name ->
                withClash {
                    setSelectProxy(group, name)
                }
            }
        }

        chipList.adapter = ProxyChipAdapter(this) {
            launch {
                mediator.scrollTo(it)
            }
        }
        chipList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        chipList.itemAnimator?.changeDuration = 0

        launch {
            mediator.exec()
        }
    }

    override fun onStart() {
        super.onStart()

        refreshList()
    }

    override suspend fun onClashStarted() {
        finish()
    }

    private fun refreshList() {
        launch {
            val proxies = withClash {
                queryAllProxyGroups()
            }

            val sorter = ProxySorter(ProxySorter.Order.DEFAULT, ProxySorter.Order.DEFAULT)

            val sorted = withContext(Dispatchers.Default) {
                sorter.sort(proxies.toList())
            }

            (mainList.adapter!! as AbstractProxyAdapter).apply {
                root = sorted

                applyChange()
            }

            (chipList.adapter!! as ProxyChipAdapter).apply {
                chips = sorted.map { it.name }
                notifyDataSetChanged()
            }
        }
    }
}