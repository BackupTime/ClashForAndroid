package com.github.kr328.clash.model

import com.github.kr328.clash.core.model.Proxy

interface ListProxy {
    data class ListProxyHeader(val name: String, val type: Proxy.Type, val now: String, var nowIndex: Int, var urlTest: Boolean = false) :
        ListProxy

    data class ListProxyItem(
        val name: String,
        val type: String,
        var delay: Long,
        val header: ListProxyHeader
    ) : ListProxy
}