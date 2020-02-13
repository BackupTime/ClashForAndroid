package com.github.kr328.clash.service.data

import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.kr328.clash.core.serialization.Parcels
import kotlinx.serialization.Serializable

@Entity(tableName = "profiles", primaryKeys = ["id"])
@Serializable
data class ClashProfileEntity(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "type") val type: Int,
    @ColumnInfo(name = "uri") val uri: String,
    @ColumnInfo(name = "source") val source: String?,
    @ColumnInfo(name = "active") val active: Boolean,
    @ColumnInfo(name = "last_update") val lastUpdate: Long,
    @ColumnInfo(name = "update_interval") val updateInterval: Long,
    @ColumnInfo(name = "id") val id: Long
) : Parcelable {
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcels.dump(serializer(), this, parcel)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object {
        const val TYPE_FILE = 1
        const val TYPE_URL = 2
        const val TYPE_EXTERNAL = 3
        const val TYPE_UNKNOWN = -1

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