package com.github.kr328.clash.service.ipc

import android.os.Parcel
import android.os.Parcelable

data class ParcelableResult(val data: Parcelable?, val exception: String?) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(ParcelableResult::class.java.classLoader),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(data, 0)
        parcel.writeString(exception)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ParcelableResult> {
        override fun createFromParcel(parcel: Parcel): ParcelableResult {
            return ParcelableResult(parcel)
        }

        override fun newArray(size: Int): Array<ParcelableResult?> {
            return arrayOfNulls(size)
        }
    }
}