package com.github.kr328.clash.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.kr328.clash.core.serialization.Parcels
import kotlinx.serialization.*
import kotlinx.serialization.internal.StringDescriptor

@Serializable
data class General(val mode: Mode, val http: Int, val socks: Int, val redirect: Int) : Parcelable {
    @Serializable(with = ModeSerializer::class)
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

    class ModeSerializer : KSerializer<Mode> {
        override val descriptor: SerialDescriptor
            get() = StringDescriptor

        override fun deserialize(decoder: Decoder): Mode {
            return when (decoder.decodeInt()) {
                MODE_DIRECT -> Mode.DIRECT
                MODE_GLOBAL -> Mode.GLOBAL
                MODE_RULE -> Mode.RULE
                else -> throw IllegalArgumentException("Invalid mode")
            }
        }

        override fun serialize(encoder: Encoder, obj: Mode) {
            when (obj) {
                Mode.DIRECT -> encoder.encodeInt(MODE_DIRECT)
                Mode.GLOBAL -> encoder.encodeInt(MODE_GLOBAL)
                Mode.RULE -> encoder.encodeInt(MODE_RULE)
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
        const val MODE_DIRECT = 1
        const val MODE_GLOBAL = 2
        const val MODE_RULE = 3

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