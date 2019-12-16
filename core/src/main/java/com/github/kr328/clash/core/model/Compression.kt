package com.github.kr328.clash.core.model

import android.os.Parcel
import android.os.Parcelable
import com.github.kr328.clash.core.serialization.Parcels
import com.github.kr328.clash.core.utils.Log
import kotlinx.serialization.Serializable
import java.lang.IllegalStateException

fun List<Proxy>.compress(): CompressedProxyList {
    val nameMap = this.toList().mapIndexed { index, proxy ->
        proxy.name to index
    }.toMap()

    val elements = this.map {
        CompressedProxyList.Element(
            name = nameMap[it.name] ?: throw IllegalStateException("Unknown proxy ${it.name}"),
            type = it.type,
            now = nameMap[it.now] ?: -1,
            all = it.all.mapNotNull { name -> nameMap[name] },
            delay = it.delay
        )
    }

    return CompressedProxyList(nameMap.entries.associate { (k, v) -> v to k }, elements)
}

@Serializable
data class CompressedProxyList(val proxyName: Map<Int, String>, val elements: List<Element>): Parcelable {
    @Serializable
    data class Element(val name: Int,
                       val type: Proxy.Type,
                       val now: Int,
                       val all: List<Int>,
                       val delay: Long)

    fun uncompress(): List<Proxy> {
        return elements.map {
            Proxy(
                name = proxyName[it.name] ?: throw IllegalStateException("Unknown proxy $it"),
                type = it.type,
                now = proxyName[it.now] ?: "",
                all = it.all.mapNotNull { index -> proxyName[index] },
                delay = it.delay
            )
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        Parcels.dump(serializer(), this, parcel)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CompressedProxyList> {
        override fun createFromParcel(parcel: Parcel): CompressedProxyList {
            return Parcels.load(serializer(), parcel)
        }

        override fun newArray(size: Int): Array<CompressedProxyList?> {
            return arrayOfNulls(size)
        }
    }
}