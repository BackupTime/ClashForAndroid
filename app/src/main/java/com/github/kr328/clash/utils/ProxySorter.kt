package com.github.kr328.clash.utils

import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxyGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProxySorter(val groupOrder: Order, val proxyOrder: Order) {
    enum class Order {
        DEFAULT, DELAY_INCREASE, DELAY_DECREASE, NAME_INCREASE, NAME_DECREASE
    }

    suspend fun sort(proxyGroup: List<ProxyGroup>): List<ProxyGroup> =
        withContext(Dispatchers.Default) {
            val global = proxyGroup.singleOrNull {
                it.name == "GLOBAL"
            }

            val sortedGroup = when (groupOrder) {
                Order.DEFAULT -> groupSortWithDefault(global, proxyGroup)
                Order.DELAY_INCREASE -> groupSortWithDelay(true, proxyGroup)
                Order.DELAY_DECREASE -> groupSortWithDelay(false, proxyGroup)
                Order.NAME_INCREASE -> groupSortWithName(true, proxyGroup)
                Order.NAME_DECREASE -> groupSortWithName(false, proxyGroup)
            }

            sortedGroup.map {
                val sortedProxy = when (proxyOrder) {
                    Order.DEFAULT -> it.proxies
                    Order.DELAY_INCREASE -> proxySortWithDelay(true, it.proxies)
                    Order.DELAY_DECREASE -> proxySortWithDelay(false, it.proxies)
                    Order.NAME_INCREASE -> proxySortWithName(true, it.proxies)
                    Order.NAME_DECREASE -> proxySortWithName(false, it.proxies)
                }

                it.copy(proxies = sortedProxy)
            }
        }

    private fun groupSortWithDefault(
        global: ProxyGroup?,
        proxyGroup: List<ProxyGroup>
    ): List<ProxyGroup> {
        if (global == null) return proxyGroup

        val orderMap = global.proxies.mapIndexed { index, proxy ->
            proxy.name to index
        }.toMap()

        return proxyGroup.sortedBy {
            orderMap[it.name] ?: Int.MAX_VALUE
        }
    }

    private fun groupSortWithName(
        increase: Boolean,
        proxyGroup: List<ProxyGroup>
    ): List<ProxyGroup> {
        return if (increase)
            proxyGroup.sortedBy { it.name }
        else
            proxyGroup.sortedByDescending { it.name }
    }

    private fun groupSortWithDelay(
        increase: Boolean,
        proxyGroup: List<ProxyGroup>
    ): List<ProxyGroup> {
        return if (increase)
            proxyGroup.sortedBy { it.delay }
        else
            proxyGroup.sortedByDescending { it.delay }
    }

    private fun proxySortWithName(
        increase: Boolean,
        proxies: List<Proxy>
    ): List<Proxy> {
        return if (increase)
            proxies.sortedBy { it.name }
        else
            proxies.sortedByDescending { it.name }
    }

    private fun proxySortWithDelay(
        increase: Boolean,
        proxies: List<Proxy>
    ): List<Proxy> {
        return if (increase)
            proxies.sortedBy { it.delay }
        else
            proxies.sortedByDescending { it.delay }
    }
}