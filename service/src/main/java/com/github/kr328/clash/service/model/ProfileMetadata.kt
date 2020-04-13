@file:UseSerializers(UriSerializer::class)

package com.github.kr328.clash.service.model

import android.net.Uri
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class ProfileMetadata(
    val id: Long,
    val name: String,
    val type: Type,
    val uri: Uri,
    val source: Uri?,
    val active: Boolean,
    val interval: Long,
    val lastModified: Long
) {
    enum class Type {
        FILE, URL, EXTERNAL, UNKNOWN
    }
}