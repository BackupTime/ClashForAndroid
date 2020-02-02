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
) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        MergedParcels.dump(serializer(), this, parcel)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ProxyGroup> {
        override fun createFromParcel(parcel: Parcel): ProxyGroup {
            return MergedParcels.load(serializer(), parcel)
        }

        override fun newArray(size: Int): Array<ProxyGroup?> {
            return arrayOfNulls(size)
        }
    }
}