package com.github.kr328.clash.service

import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.service.data.ClashDatabase
import com.github.kr328.clash.service.data.ClashProfileEntity
import com.github.kr328.clash.service.ipc.ParcelableCompletedFuture

class ClashProfileManager(private val database: ClashDatabase): IClashProfileManager.Stub() {
    override fun updateProfile(id: Int): ParcelableCompletedFuture {
        val entity = database.openClashProfileDao().queryProfileById(id)

        require(entity != null && entity.type == ClashProfileEntity.Type.URL)

        val result = ParcelableCompletedFuture()
    }

    override fun queryAllProfiles(): ParcelableCompletedFuture {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addProfile(url: String?): ParcelableCompletedFuture {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}