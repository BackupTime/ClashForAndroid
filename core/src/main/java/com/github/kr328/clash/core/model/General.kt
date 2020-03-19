package com.github.kr328.clash.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.kr328.clash.core.serialization.Parcels
import kotlinx.serialization.*

@Serializable
data class General(val mode: Mode, val http: Int, val socks: Int, val redirect: Int) : Parcelable {
    @Serializable
    enum class Mode {
        DIRECT, GLOBAL, RULE;

        override fun toString(): String {
            return when (this) {
                DIRECT -> "Direct"
                GLOBAL -> "Global"
                RULE -> "Rule"
            }
        }

        companion object {
            fun fromString(mode: String): Mode {
                return when (mode) {
                    "Direct" -> DIRECT
                    "Global" -> GLOBAL
                    "Rule" -> RULE
                    else -> throw IllegalArgumentException("Invalid mode $mode")
                }
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcels.dump(serializer(), this, parcel)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<General> {
            override fun createFromParcel(parcel: Parcel): General {
                return Parcels.load(serializer(), parcel)
            }

            override fun newArray(size: Int): Array<General?> {
                return arrayOfNulls(size)
            }
        }
    }
}