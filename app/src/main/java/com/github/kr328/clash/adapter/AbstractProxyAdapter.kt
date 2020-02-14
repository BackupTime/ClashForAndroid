package com.github.kr328.clash.adapter

import com.github.kr328.clash.core.model.ProxyGroup

interface AbstractProxyAdapter {
    var root: List<ProxyGroup>
    var onSelectProxyListener: suspend (String, String) -> Unit

    suspend fun applyChange()
    suspend fun getGroupPosition(name: String): Int?
    suspend fun getCurrentGroup(): String
}