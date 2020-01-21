package com.github.kr328.clash.service.ipc

import android.os.Parcel
import android.os.Parcelable
import android.os.RemoteException
import java.util.concurrent.CompletableFuture

class ParcelableCompletedFuture private constructor(private val pipe: ParcelablePipe) :
    CompletableFuture<Parcelable?>(), Parcelable {
    constructor() : this(ParcelablePipe())
    constructor(parcel: Parcel) : this(
        parcel.readParcelable<ParcelablePipe>(
            ParcelableCompletedFuture::class.java.classLoader
        ) ?: throw NullPointerException()
    ) {
        pipe.onReceive {
            val result = (it ?: throw NullPointerException()) as ParcelableResult

            if (result.exception != null) {
                completeExceptionally(RemoteException(result.exception))
            } else {
                complete(result.data)
            }
        }
    }

    override fun complete(value: Parcelable?): Boolean {
        if (super.complete(value)) {
            pipe.send(ParcelableResult(value, null))
            return true
        }
        return false
    }

    override fun completeExceptionally(ex: Throwable?): Boolean {
        if (super.completeExceptionally(ex)) {
            pipe.send(ParcelableResult(null, ex?.message ?: "Unknown"))
            return true
        }
        return false
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(pipe, 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ParcelableCompletedFuture> {
        override fun createFromParcel(parcel: Parcel): ParcelableCompletedFuture {
            return ParcelableCompletedFuture(parcel)
        }

        override fun newArray(size: Int): Array<ParcelableCompletedFuture?> {
            return arrayOfNulls(size)
        }
    }
}