package com.github.kr328.clash

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.adapter.AbstractProxyAdapter
import com.github.kr328.clash.adapter.GridProxyAdapter
import com.github.kr328.clash.adapter.ProxyChipAdapter
import com.github.kr328.clash.core.model.General
import com.github.kr328.clash.core.utils.Log
import com.github.kr328.clash.preference.UiPreferences
import com.github.kr328.clash.remote.withClash
import com.github.kr328.clash.utils.ProxySorter
import com.github.kr328.clash.view.ProxiesTabMediator
import kotlinx.android.synthetic.main.activity_proxies.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProxiesActivity : BaseActivity() {
    private lateinit var mediator: ProxiesTabMediator
    private val doScrollToLastProxy by lazy {
        val selected = uiPreference.get(UiPreferences.LAST_SELECT_GROUP)

        launch {
            mediator.scrollToDirect(selected)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_proxies)
        setSupportActionBar(toolbar)

        mediator = ProxiesTabMediator(this, mainList, chipList)

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

        refreshList()
    }

    override fun onStop() {
        uiPreference.edit {
            put(UiPreferences.LAST_SELECT_GROUP, (chipList.adapter!! as ProxyChipAdapter).selected)
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
                    uiPreference.edit {
                        put(UiPreferences.PROXY_GROUP_SORT, UiPreferences.PROXY_SORT_DEFAULT)
                    }
                }
                R.id.groupName -> {
                    uiPreference.edit {
                        put(UiPreferences.PROXY_GROUP_SORT, UiPreferences.PROXY_SORT_NAME)
                    }
                }
                R.id.groupDelay -> {
                    uiPreference.edit {
                        put(UiPreferences.PROXY_GROUP_SORT, UiPreferences.PROXY_SORT_DELAY)
                    }
                }
                R.id.proxyDefault -> {
                    uiPreference.edit {
                        put(UiPreferences.PROXY_PROXY_SORT, UiPreferences.PROXY_SORT_DEFAULT)
                    }
                }
                R.id.proxyName -> {
                    uiPreference.edit {
                        put(UiPreferences.PROXY_PROXY_SORT, UiPreferences.PROXY_SORT_NAME)
                    }
                }
                R.id.proxyDelay -> {
                    uiPreference.edit {
                        put(UiPreferences.PROXY_PROXY_SORT, UiPreferences.PROXY_SORT_DELAY)
                    }
                }
                else -> return@launch
            }

            item.isChecked = true

            refreshList()
        }

        return true
    }

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
                when (uiPreference.get(UiPreferences.PROXY_GROUP_SORT)) {
                    UiPreferences.PROXY_SORT_DEFAULT ->
                        findItem(R.id.groupDefault).isChecked = true
                    UiPreferences.PROXY_SORT_NAME ->
                        findItem(R.id.groupName).isChecked = true
                    UiPreferences.PROXY_SORT_DELAY ->
                        findItem(R.id.proxyDelay).isChecked = true
                }
                when (uiPreference.get(UiPreferences.PROXY_PROXY_SORT)) {
                    UiPreferences.PROXY_SORT_DEFAULT ->
                        findItem(R.id.proxyDefault).isChecked = true
                    UiPreferences.PROXY_SORT_NAME ->
                        findItem(R.id.proxyName).isChecked = true
                    UiPreferences.PROXY_SORT_DELAY ->
                        findItem(R.id.proxyDelay).isChecked = true
                }
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

            val groupSort = when (uiPreference.get(UiPreferences.PROXY_GROUP_SORT)) {
                UiPreferences.PROXY_SORT_DEFAULT ->
                    ProxySorter.Order.DEFAULT
                UiPreferences.PROXY_SORT_NAME ->
                    ProxySorter.Order.NAME_INCREASE
                UiPreferences.PROXY_SORT_DELAY ->
                    ProxySorter.Order.DELAY_INCREASE
                else -> throw IllegalArgumentException()
            }

            val proxySort = when (uiPreference.get(UiPreferences.PROXY_PROXY_SORT)) {
                UiPreferences.PROXY_SORT_DEFAULT ->
                    ProxySorter.Order.DEFAULT
                UiPreferences.PROXY_SORT_NAME ->
                    ProxySorter.Order.NAME_INCREASE
                UiPreferences.PROXY_SORT_DELAY ->
                    ProxySorter.Order.DELAY_INCREASE
                else -> throw IllegalArgumentException()
            }

            val sorter = ProxySorter(groupSort, proxySort)

            val sorted = withContext(Dispatchers.Default) {
                sorter.sort(proxies.toList())
            }.run {
                when (general.mode) {
                    General.Mode.GLOBAL -> this
                    General.Mode.DIRECT -> emptyList()
                    General.Mode.RULE -> this.filter { it.name != "GLOBAL" }
                }
            }

            (mainList.adapter!! as AbstractProxyAdapter).apply {
                root = sorted

                applyChange()
            }

            (chipList.adapter!! as ProxyChipAdapter).apply {
                chips = sorted.map { it.name }
                notifyDataSetChanged()
            }

            doScrollToLastProxy
        }
    }
}