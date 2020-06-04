package com.github.kr328.clash.core.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

@Serializable
data class ProxyGroup(
    val name: String,
    val type: Proxy.Type,
    val delay: Long,
    val current: String,
    val proxies: List<Proxy>
) {
    @Keep
    constructor(name: String, type: String, delay: Long, current: String, proxies: Array<Proxy>) :
            this(name, Proxy.Type.fromString(type), delay, current, proxies.toList())
}