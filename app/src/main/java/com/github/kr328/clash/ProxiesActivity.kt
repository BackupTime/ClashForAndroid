package com.github.kr328.clash

import android.os.Bundle
import androidx.core.view.isEmpty
import com.github.kr328.clash.adapter.AbstractProxyAdapter
import com.github.kr328.clash.adapter.GridProxyAdapter
import com.github.kr328.clash.core.model.ProxyGroup
import com.github.kr328.clash.core.utils.Log
import com.github.kr328.clash.remote.withClash
import com.github.kr328.clash.utils.ProxySorter
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.activity_proxies.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProxiesActivity : BaseActivity() {
    private val activity: ProxiesActivity
        get() = this
    private var maskedGroup: MutableSet<String> = mutableSetOf()

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

            if (chipGroup.isEmpty()) {
                filtered.map(ProxyGroup::name).forEach {
                    val chip = Chip(activity).apply {
                        text = it

                        chipBackgroundColor = getColorStateList(R.color.proxies_chip_colors)
                        rippleColor = getColorStateList(R.color.proxies_chip_colors)
                        setTextColor(getColorStateList(R.color.proxies_chip_text_colors))
                        checkedIcon = null

                        isCheckable = true
                        isClickable = true
                        isFocusable = true
                        isChecked = !maskedGroup.contains(it)

                        setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                maskedGroup.remove(it)
                            } else {
                                maskedGroup.add(it)
                            }

                            refreshList(it)
                        }
                    }

                    chipGroup.addView(chip)
                }
            }

            (mainList.adapter!! as AbstractProxyAdapter).apply {
                root = filtered.filter { !maskedGroup.contains(it.name) }

                applyChange()

                if (scrollTo != null) {
                    scrollToGroup(scrollTo)
                }
            }
        }
    }
}