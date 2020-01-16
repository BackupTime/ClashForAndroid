package com.github.kr328.clash.service.ipc

import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable

class ParcelablePipe() : Parcelable {
    private inner class Slave: Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if ( code == TRANSACT_CODE_SEND_PARCELABLE ) {
                receiveCallback(data.readParcelable(ParcelablePipe::class.java.classLoader))
                return true
            }
            return false
        }
    }
    private inner class Master: Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if ( code == TRANSACT_CODE_SET_SLAVE ) {
                slave = data.readStrongBinder()
                return true
            }
            return false
        }
    }

    private var slave: IBinder? = null
    private var receiveCallback: (Parcelable?) -> Unit = {}

    constructor(parcel: Parcel) : this() {
        val master = parcel.readStrongBinder()
        val data = Parcel.obtain()

        try {
            data.writeStrongBinder(Slave())

            master.transact(TRANSACT_CODE_SET_SLAVE, data, null, 0)
        }
        finally {
            data.recycle()
        }
    }

    override fun writeToParcel(dest: Parcel?, flags: Int) {
        if ( dest == null )
            return

        dest.writeStrongBinder(Master())
    }

    override fun describeContents(): Int = 0

    fun send(parcelable: Parcelable?): Boolean {
        val s = slave ?: return false
        val data = Parcel.obtain()

        try {
            data.writeParcelable(parcelable, 0)

            s.transact(TRANSACT_CODE_SEND_PARCELABLE, data, null, 0)
        }
        finally {
            data.recycle()
        }

        return true
    }

    fun onReceive(callback: (Parcelable?) -> Unit) {
        this.receiveCallback = callback
    }

    companion object {
        private const val TRANSACT_CODE_SET_SLAVE = 1
        private const val TRANSACT_CODE_SEND_PARCELABLE = 2

        @JvmField
        val CREATOR = object : Parcelable.Creator<ParcelablePipe> {
            override fun createFromParcel(parcel: Parcel): ParcelablePipe {
                return ParcelablePipe(parcel)
            }

            override fun newArray(size: Int): Array<ParcelablePipe?> {
                return arrayOfNulls(size)
            }
        }
    }
}