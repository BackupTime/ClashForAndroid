package com.github.kr328.clash.core.model

import kotlinx.serialization.Serializable

@Serializable
data class ProxyGroup(
    val name: String,
    val type: Proxy.Type,
    val delay: Long,
    val current: String,
    val proxies: List<Proxy>
)