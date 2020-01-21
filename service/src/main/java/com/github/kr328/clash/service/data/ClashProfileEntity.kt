package com.github.kr328.clash.service.data

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.github.kr328.clash.core.serialization.Parcels
import kotlinx.serialization.Serializable

@Entity(tableName = "profiles")
@Serializable
data class ClashProfileEntity(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "type") val type: Type,
    @ColumnInfo(name = "uri") val uri: String,
    @ColumnInfo(name = "file") val file: String,
    @ColumnInfo(name = "base") val base: String,
    @ColumnInfo(name = "active") val active: Boolean,
    @ColumnInfo(name = "last_update") val lastUpdate: Long,
    @ColumnInfo(name = "id") @PrimaryKey(autoGenerate = true) val id: Int = 0
) : Parcelable {
    enum class Type(val id: Int) {
        URL(1), FILE(2), UNKNOWN(-1)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcels.dump(serializer(), this, parcel)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        @TypeConverter
        fun typeToInt(value: Type?): Int {
            return value?.id ?: -1
        }

        @TypeConverter
        fun intToType(value: Int?): Type {
            return when (value) {
                Type.URL.id -> Type.URL
                Type.FILE.id -> Type.FILE
                Type.UNKNOWN.id -> Type.UNKNOWN
                else -> Type.UNKNOWN
            }
        }

        @JvmField
        val CREATOR = object : Parcelable.Creator<ClashProfileEntity> {
            override fun createFromParcel(parcel: Parcel): ClashProfileEntity {
                return Parcels.load(serializer(), parcel)
            }

            override fun newArray(size: Int): Array<ClashProfileEntity?> {
                return arrayOfNulls(size)
            }
        }
    }
}