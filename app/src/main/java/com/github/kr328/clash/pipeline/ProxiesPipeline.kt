package com.github.kr328.clash.pipeline

import com.github.kr328.clash.adapter.ProxyAdapter
import com.github.kr328.clash.core.model.General
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxyGroup
import com.github.kr328.clash.preference.UiSettings
import com.github.kr328.clash.utils.PrefixMerger
import com.github.kr328.clash.utils.ProxySorter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

data class ProxyEntry(val group: String, val name: String)
data class ProxyMerged(val prefix: String, val content: String)

suspend fun Pipeline<List<ProxyGroup>>.mergePrefix(): Pipeline<Map<ProxyEntry, ProxyMerged>> {
    if (!settings.get(UiSettings.PROXY_MERGE_PREFIX))
        return Pipeline(emptyMap(), settings)

    val result = coroutineScope {
        input
            .map {
                async {
                    it.name to PrefixMerger.merge(it.proxies, Proxy::name)
                }
            }
            .map {
                it.await()
            }
            .flatMap {
                it.second.map { merged ->
                    ProxyEntry(it.first, merged.value.name) to ProxyMerged(
                        merged.prefix,
                        merged.content
                    )
                }
            }
            .toMap()
    }

    return Pipeline(result, settings)
}

suspend fun Pipeline<List<ProxyGroup>>.sort(): Pipeline<List<ProxyGroup>> {
    val groupSort = when (settings.get(UiSettings.PROXY_GROUP_SORT)) {
        UiSettings.PROXY_SORT_DEFAULT ->
            ProxySorter.Order.DEFAULT
        UiSettings.PROXY_SORT_NAME ->
            ProxySorter.Order.NAME_INCREASE
        UiSettings.PROXY_SORT_DELAY ->
            ProxySorter.Order.DELAY_INCREASE
        else -> throw IllegalArgumentException()
    }

    val proxySort = when (settings.get(UiSettings.PROXY_PROXY_SORT)) {
        UiSettings.PROXY_SORT_DEFAULT ->
            ProxySorter.Order.DEFAULT
        UiSettings.PROXY_SORT_NAME ->
            ProxySorter.Order.NAME_INCREASE
        UiSettings.PROXY_SORT_DELAY ->
            ProxySorter.Order.DELAY_INCREASE
        else -> throw IllegalArgumentException()
    }

    val sorter = ProxySorter(groupSort, proxySort)

    return copy(input = sorter.sort(input))
}

suspend fun Pipeline<List<ProxyGroup>>.toAdapterElement(
    prefixMerged: Map<ProxyEntry, ProxyMerged>,
    general: General
): List<ProxyAdapter.ProxyGroupInfo> {
    return input.map { group ->
        val proxies = group.proxies.map { proxy ->
            val merged = prefixMerged[ProxyEntry(group.name, proxy.name)]?.takeIf {
                it.prefix.isNotBlank() && it.content.isNotBlank()
            } ?: ProxyMerged(proxy.type.toString(), proxy.name)

            ProxyAdapter.ProxyInfo(
                proxy.name, group.name, merged.content, merged.prefix,
                proxy.delay.toShort(), group.type == Proxy.Type.SELECT,
                group.current == proxy.name
            )
        }


        ProxyAdapter.ProxyGroupInfo(group.name, group.current, proxies)
    }.let {
        withContext(Dispatchers.Default) {
            when (general.mode) {
                General.Mode.DIRECT -> emptyList()
                General.Mode.GLOBAL -> it
                General.Mode.RULE -> it.filter { it.name != "GLOBAL" }
            }
        }
    }
}