package com.github.kr328.clash.service.model

import android.content.Context
import android.net.Uri
import com.github.kr328.clash.service.data.ProfileEntity
import com.github.kr328.clash.service.util.resolveProfileFile

fun ProfileEntity.toProfileMetadata(context: Context): ProfileMetadata {
    val type = when (this.type) {
        ProfileEntity.TYPE_FILE -> ProfileMetadata.Type.FILE
        ProfileEntity.TYPE_URL -> ProfileMetadata.Type.URL
        ProfileEntity.TYPE_EXTERNAL -> ProfileMetadata.Type.EXTERNAL
        else -> ProfileMetadata.Type.EXTERNAL
    }
    val lastModified = context.resolveProfileFile(id).lastModified()

    return ProfileMetadata(
        id = id,
        name = name,
        type = type,
        uri = Uri.parse(uri),
        source = source?.let { Uri.parse(it)},
        active = active,
        interval = interval,
        lastModified = lastModified
    )
}

fun ProfileMetadata.toProfileEntity(): ProfileEntity {
    val type = when (this.type) {
        ProfileMetadata.Type.FILE -> ProfileEntity.TYPE_FILE
        ProfileMetadata.Type.URL -> ProfileEntity.TYPE_URL
        ProfileMetadata.Type.EXTERNAL -> ProfileEntity.TYPE_EXTERNAL
        ProfileMetadata.Type.UNKNOWN -> ProfileEntity.TYPE_UNKNOWN
    }

    return ProfileEntity(
        name = name,
        type = type,
        uri = uri.toString(),
        source = source?.toString(),
        active = active,
        interval = interval,
        id = id
    )
}