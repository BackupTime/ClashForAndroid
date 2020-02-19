package com.github.kr328.clash

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.adapter.ProxyAdapter
import com.github.kr328.clash.adapter.ProxyChipAdapter
import com.github.kr328.clash.core.model.General
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.preference.UiSettings
import com.github.kr328.clash.remote.withClash
import com.github.kr328.clash.utils.PrefixMerger
import com.github.kr328.clash.utils.ProxySorter
import com.github.kr328.clash.utils.ScrollBinding
import kotlinx.android.synthetic.main.activity_proxies.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProxiesActivity : BaseActivity(), ScrollBinding.Callback {
    private val scrollBinding = ScrollBinding(this, this)
    private val doScrollToLastProxy by lazy {
        val selected = uiSettings.get(UiSettings.PROXY_LAST_SELECT_GROUP)

        launch {
            scrollBinding.scrollMaster(selected)
        }
    }

    private val mainListAdapter: ProxyAdapter
        get() = mainList.adapter as ProxyAdapter
    private val chipListAdapter: ProxyChipAdapter
        get() = chipList.adapter as ProxyChipAdapter
    private val urlTesting: MutableSet<String> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proxies)
        setSupportActionBar(toolbar)

        mainList.adapter = ProxyAdapter(this, { group, proxy ->
            launch {
                withClash {
                    setSelectProxy(group, proxy)
                }
            }
        }, {
            launch {
                urlTesting.add(it)

                withClash {
                    urlTesting.add(it)

                    startHealthCheck(it)

                    urlTesting.remove(it)

                    refreshList()
                }
            }
        })

        mainList.layoutManager = mainListAdapter.layoutManager

        chipList.adapter = ProxyChipAdapter(this) {
            launch {
                scrollBinding.scrollMaster(it)
            }
        }
        chipList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        chipList.itemAnimator?.changeDuration = 0

        launch {
            mainList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    scrollBinding.sendMasterScrolled()
                }
            })

            scrollBinding.exec()
        }

        refreshList()
    }

    override fun onStop() {
        uiSettings.commit {
            put(UiSettings.PROXY_LAST_SELECT_GROUP,
                (chipList.adapter!! as ProxyChipAdapter).selected)
        }

        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (!super.onCreateOptionsMenu(menu))
            return false

        menuInflater.inflate(R.menu.proxies, menu)

        setupMenu()

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (super.onOptionsItemSelected(item))
            return true

        if (item.itemId == R.id.menuRefresh) {
            refreshList()
            return true
        }

        launch {
            when (item.itemId) {
                R.id.modeDirect -> {
                    withClash {
                        setProxyMode(General.Mode.DIRECT)
                    }
                }
                R.id.modeGlobal -> {
                    withClash {
                        setProxyMode(General.Mode.GLOBAL)
                    }
                }
                R.id.modeRule -> {
                    withClash {
                        setProxyMode(General.Mode.RULE)
                    }
                }
                R.id.groupDefault -> {
                    uiSettings.commit {
                        put(UiSettings.PROXY_GROUP_SORT, UiSettings.PROXY_SORT_DEFAULT)
                    }
                }
                R.id.groupName -> {
                    uiSettings.commit {
                        put(UiSettings.PROXY_GROUP_SORT, UiSettings.PROXY_SORT_NAME)
                    }
                }
                R.id.groupDelay -> {
                    uiSettings.commit {
                        put(UiSettings.PROXY_GROUP_SORT, UiSettings.PROXY_SORT_DELAY)
                    }
                }
                R.id.proxyDefault -> {
                    uiSettings.commit {
                        put(UiSettings.PROXY_PROXY_SORT, UiSettings.PROXY_SORT_DEFAULT)
                    }
                }
                R.id.proxyName -> {
                    uiSettings.commit {
                        put(UiSettings.PROXY_PROXY_SORT, UiSettings.PROXY_SORT_NAME)
                    }
                }
                R.id.proxyDelay -> {
                    uiSettings.commit {
                        put(UiSettings.PROXY_PROXY_SORT, UiSettings.PROXY_SORT_DELAY)
                    }
                }
                R.id.utilsMergePrefix -> {
                    item.isChecked = !item.isChecked

                    uiSettings.commit {
                        put(UiSettings.PROXY_MERGE_PREFIX, item.isChecked)
                    }

                    refreshList()

                    return@launch
                }
                else -> return@launch
            }

            item.isChecked = true

            refreshList()
        }

        return true
    }

    override val activityLabel: CharSequence?
        get() = getText(R.string.proxy)

    override suspend fun onClashStarted() {
        finish()
    }

    private fun setupMenu() {
        launch {
            val general = withClash {
                queryGeneral()
            }

            menu?.apply {
                when (general.mode) {
                    General.Mode.DIRECT ->
                        findItem(R.id.modeDirect).isChecked = true
                    General.Mode.GLOBAL ->
                        findItem(R.id.modeGlobal).isChecked = true
                    General.Mode.RULE ->
                        findItem(R.id.modeRule).isChecked = true
                }
                when (uiSettings.get(UiSettings.PROXY_GROUP_SORT)) {
                    UiSettings.PROXY_SORT_DEFAULT ->
                        findItem(R.id.groupDefault).isChecked = true
                    UiSettings.PROXY_SORT_NAME ->
                        findItem(R.id.groupName).isChecked = true
                    UiSettings.PROXY_SORT_DELAY ->
                        findItem(R.id.proxyDelay).isChecked = true
                }
                when (uiSettings.get(UiSettings.PROXY_PROXY_SORT)) {
                    UiSettings.PROXY_SORT_DEFAULT ->
                        findItem(R.id.proxyDefault).isChecked = true
                    UiSettings.PROXY_SORT_NAME ->
                        findItem(R.id.proxyName).isChecked = true
                    UiSettings.PROXY_SORT_DELAY ->
                        findItem(R.id.proxyDelay).isChecked = true
                }

                findItem(R.id.utilsMergePrefix).isChecked =
                    uiSettings.get(UiSettings.PROXY_MERGE_PREFIX)
            }
        }
    }

    private fun refreshList() {
        launch {
            val general = withClash {
                queryGeneral()
            }
            val proxies = withClash {
                queryAllProxyGroups()
            }

            val prefixDeferred = async {
                if (uiSettings.get(UiSettings.PROXY_MERGE_PREFIX)) {
                    proxies.map {
                        async { PrefixMerger.merge(it.proxies.map { p -> it.name to p.name }) { it.second } }
                    }.flatMap {
                        it.await()
                    }.map {
                        it.value to it
                    }.toMap()
                } else emptyMap()
            }

            val sortDeferred = async {
                val groupSort = when (uiSettings.get(UiSettings.PROXY_GROUP_SORT)) {
                    UiSettings.PROXY_SORT_DEFAULT ->
                        ProxySorter.Order.DEFAULT
                    UiSettings.PROXY_SORT_NAME ->
                        ProxySorter.Order.NAME_INCREASE
                    UiSettings.PROXY_SORT_DELAY ->
                        ProxySorter.Order.DELAY_INCREASE
                    else -> throw IllegalArgumentException()
                }

                val proxySort = when (uiSettings.get(UiSettings.PROXY_PROXY_SORT)) {
                    UiSettings.PROXY_SORT_DEFAULT ->
                        ProxySorter.Order.DEFAULT
                    UiSettings.PROXY_SORT_NAME ->
                        ProxySorter.Order.NAME_INCREASE
                    UiSettings.PROXY_SORT_DELAY ->
                        ProxySorter.Order.DELAY_INCREASE
                    else -> throw IllegalArgumentException()
                }

                val sorter = ProxySorter(groupSort, proxySort)

                sorter.sort(proxies).run {
                    when (general.mode) {
                        General.Mode.GLOBAL -> this
                        General.Mode.DIRECT -> emptyList()
                        General.Mode.RULE -> this.filter { it.name != "GLOBAL" }
                    }
                }
            }

            val prefix = prefixDeferred.await()
            val sorted = sortDeferred.await()

            val newList = withContext(Dispatchers.Default) {
                sorted.map {
                    ProxyAdapter.ProxyGroupInfo(it.name,
                        it.proxies.map { p ->
                            val r = prefix.getOrElse(it.name to p.name) {
                                PrefixMerger.Result(p.name, "", p)
                            }

                            val data = if ( r.content.isEmpty() ) {
                                r.prefix to p.type.toString()
                            }
                            else {
                                r.content to r.prefix
                            }

                            ProxyAdapter.ProxyInfo(
                                p.name,
                                it.name,
                                data.first,
                                data.second,
                                p.delay.toShort(),
                                it.type == Proxy.Type.SELECT,
                                p.name == it.current
                            )
                        }
                    )
                }
            }

            mainListAdapter.applyChange(newList, urlTesting)

            (chipList.adapter!! as ProxyChipAdapter).apply {
                chips = sorted.map { it.name }
                notifyDataSetChanged()
            }

            doScrollToLastProxy
        }
    }

    override fun getCurrentMasterToken(): String {
        return mainListAdapter.getCurrentGroup()
    }

    override fun onMasterTokenChanged(token: String) {
        chipListAdapter.selected = token
        val position = chipListAdapter.chips.indexOf(token)

        if (position < 0)
            return

        chipList.smoothScrollToPosition(position)
    }

    override fun getMasterTokenPosition(token: String): Int {
        return mainListAdapter.getGroupPosition(token)
    }

    override fun doMasterScroll(scroller: LinearSmoothScroller) {
        mainListAdapter.layoutManager.startSmoothScroll(scroller)
    }
}