package com.github.kr328.clash.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.kr328.clash.core.serialization.MergedParcels
import kotlinx.serialization.Serializable

@Serializable
data class ProxyGroup(
    val name: String,
    val type: Proxy.Type,
    val delay: Long,
    val current: String,
    val proxies: List<Proxy>
)