@file:UseSerializers(UriSerializer::class)

package com.github.kr328.clash.service.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.github.kr328.clash.common.serialization.Parcels
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
): Parcelable {
    enum class Type {
        FILE, URL, EXTERNAL, UNKNOWN
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcels.dump(serializer(), this, parcel)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ProfileMetadata> {
        override fun createFromParcel(parcel: Parcel): ProfileMetadata {
            return Parcels.load(serializer(), parcel)
        }

        override fun newArray(size: Int): Array<ProfileMetadata?> {
            return arrayOfNulls(size)
        }
    }
}