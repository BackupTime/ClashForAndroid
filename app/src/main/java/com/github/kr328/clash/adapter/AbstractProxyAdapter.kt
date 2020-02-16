package com.github.kr328.clash.adapter

interface AbstractProxyAdapter {
    data class ProxyGroupInfo(
        val name: String,
        val proxies: List<ProxyInfo>
    )
    data class ProxyInfo(
        val name: String,
        val group: String,
        val prefix: String,
        val content: String,
        val delay: Short,
        val selectable: Boolean,
        val active: Boolean
    )

    var onSelectProxyListener: suspend (String, String) -> Unit

    suspend fun applyChange(newList: List<ProxyGroupInfo>)
    suspend fun getGroupPosition(name: String): Int?
    suspend fun getCurrentGroup(): String
}