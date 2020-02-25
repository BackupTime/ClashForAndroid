package com.github.kr328.clash.remote

import com.github.kr328.clash.service.IProfileService
import com.github.kr328.clash.service.data.ClashProfileEntity
import com.github.kr328.clash.service.transact.ProfileRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProfileClient(private val service: IProfileService) {
    suspend fun queryProfiles(): Array<ClashProfileEntity> = withContext(Dispatchers.IO) {
        Utils.callRemote {
            service.queryProfiles()
        }
    }

    suspend fun queryActiveProfile(): ClashProfileEntity? = withContext(Dispatchers.IO) {
        Utils.callRemote {
            service.queryActiveProfile()
        }
    }

    suspend fun enqueueRequest(request: ProfileRequest) = withContext(Dispatchers.IO) {
        Utils.callRemote {
            service.enqueueRequest(request)
        }
    }

    suspend fun setActiveProfile(id: Long) = withContext(Dispatchers.IO) {
        Utils.callRemote {
            service.setActiveProfile(id)
        }
    }

    suspend fun requestProfileEditUri(id: Long): String? = withContext(Dispatchers.IO) {
        Utils.callRemote {
            service.requestProfileEditUri(id)
        }
    }

    suspend fun commitProfileEditUri(uri: String) = withContext(Dispatchers.IO) {
        Utils.callRemote {
            service.commitProfileEditUri(uri)
        }
    }
}