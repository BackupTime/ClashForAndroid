package com.github.kr328.clash.service.transact

import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import com.github.kr328.clash.service.ipc.IStreamCallback

class ProfileRequest private constructor(private val bundle: Bundle) : Parcelable {
    constructor() : this(Bundle())
    constructor(parcel: Parcel) : this(
        parcel.readBundle(ProfileRequest::class.java.classLoader)
            ?: throw NullPointerException("Empty bundle")
    )

    enum class Action {
        UPDATE_OR_CREATE, REMOVE, CLEAR
    }

    val action: Action
        get() = Action.valueOf(requireNotNull(bundle.getString(KEY_ACTION)))
    val type: Int
        get() = bundle.getInt(KEY_TYPE, -1)
    val id: Long
        get() = bundle.getLong(KEY_ID, 0)
    val name: String?
        get() = bundle.getString(KEY_NAME)
    val url: Uri?
        get() = bundle.getParcelable(KEY_URL)
    val source: Uri?
        get() = bundle.getParcelable(KEY_SOURCE)
    val interval: Long
        get() = bundle.getLong(KEY_UPDATE_INTERVAL, -1)
    val callback: IStreamCallback?
        get() = IStreamCallback.Stub.asInterface(bundle.getBinder(KEY_CALLBACK))

    fun action(action: Action): ProfileRequest {
        return apply {
            bundle.putString(KEY_ACTION, action.toString())
        }
    }

    fun withType(type: Int): ProfileRequest {
        return apply {
            bundle.putInt(KEY_TYPE, type)
        }
    }

    fun withId(id: Long): ProfileRequest {
        return apply {
            bundle.putLong(KEY_ID, id)
        }
    }

    fun withName(name: String): ProfileRequest {
        return apply {
            bundle.putString(KEY_NAME, name)
        }
    }

    fun withURL(url: Uri): ProfileRequest {
        return apply {
            bundle.putParcelable(KEY_URL, url)
        }
    }

    fun withSource(source: Uri?): ProfileRequest {
        return apply {
            bundle.putParcelable(KEY_SOURCE, source)
        }
    }

    fun withUpdateInterval(interval: Long): ProfileRequest {
        return apply {
            bundle.putLong(KEY_UPDATE_INTERVAL, interval)
        }
    }

    fun withCallback(callback: IStreamCallback.Stub): ProfileRequest {
        return apply {
            bundle.putBinder(KEY_CALLBACK, callback)
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeBundle(bundle)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        private const val KEY_ACTION = "action"
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_URL = "url"
        private const val KEY_SOURCE = "source"
        private const val KEY_TYPE = "type"
        private const val KEY_CALLBACK = "callback"
        private const val KEY_UPDATE_INTERVAL = "update_interval"

        @JvmField
        val CREATOR = object : Parcelable.Creator<ProfileRequest> {
            override fun createFromParcel(parcel: Parcel): ProfileRequest {
                return ProfileRequest(parcel)
            }

            override fun newArray(size: Int): Array<ProfileRequest?> {
                return arrayOfNulls(size)
            }
        }
    }
}