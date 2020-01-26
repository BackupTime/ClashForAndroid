package com.github.kr328.clash.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.service.data.ClashDatabase
import com.github.kr328.clash.service.data.ClashProfileEntity
import com.github.kr328.clash.service.ipc.IStreamCallback
import com.github.kr328.clash.service.ipc.ParcelableCompletedFuture
import com.github.kr328.clash.service.util.DefaultThreadPool
import com.github.kr328.clash.service.util.FileUtils
import com.github.kr328.clash.service.util.sendBroadcastSelf

class ClashProfileManager(private val context: Context, private val database: ClashDatabase) :
    IClashProfileManager.Stub() {
    private val clashDir = context.filesDir.resolve(Constants.CLASH_DIR)
    private val profileDir = context.filesDir.resolve(Constants.PROFILES_DIR)

    override fun updateProfile(id: Int, callback: IStreamCallback?) {
        val entity = database.openClashProfileDao().queryProfileById(id)

        require(
            entity != null && callback != null && (entity.type == ClashProfileEntity.TYPE_URL ||
                    entity.type == ClashProfileEntity.TYPE_FILE)
        )

        downloadProfile(Uri.parse(entity.uri), entity.file, entity.base,
            onSuccess = {
                callback.complete()

                database.openClashProfileDao().touchProfile(id)

                sendChangedBroadcast()
            },
            onFailure = {
                callback.completeExceptionally(it.message)
            })
    }

    override fun queryAllProfiles(): Array<ClashProfileEntity> {
        return database.openClashProfileDao().queryProfiles()
    }

    override fun addProfile(name: String, type: Int, uri: String?, callback: IStreamCallback?) {
        require(uri != null && callback != null && (uri.startsWith("http") || uri.startsWith("content")))

        val fileName = FileUtils.generateRandomFileName(profileDir, ".yaml")
        val baseDirName = FileUtils.generateRandomFileName(clashDir)

        downloadProfile(Uri.parse(uri), fileName, baseDirName,
            onSuccess = {
                database.openClashProfileDao().addProfile(
                    ClashProfileEntity(
                        name,
                        type,
                        uri,
                        fileName,
                        baseDirName,
                        false,
                        System.currentTimeMillis()
                    )
                )

                sendChangedBroadcast()

                callback.complete()
            },
            onFailure = {
                callback.completeExceptionally(it.message)
            })
    }

    private fun downloadProfile(
        uri: Uri?,
        fileName: String,
        baseDirName: String,
        onSuccess: () -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        require(uri != null && uri != Uri.EMPTY)

        if (uri.scheme == "http" || uri.scheme == "https") {
            Clash.downloadProfile(
                uri.toString(),
                profileDir.resolve(fileName),
                clashDir.resolve(baseDirName)
            )
                .whenComplete { _, u ->
                    if (u != null)
                        onFailure(u)
                    else
                        onSuccess()
                }
        } else {
            DefaultThreadPool.submit {
                try {
                    val input = context.contentResolver.openInputStream(uri)
                        ?: throw NullPointerException("Unable to open profile")

                    input.use {
                        Clash.saveProfile(
                            it.readBytes(),
                            profileDir.resolve(fileName),
                            clashDir.resolve(baseDirName)
                        )
                    }

                    onSuccess()
                } catch (e: Exception) {
                    onFailure(e)
                }
            }
        }
    }

    private fun sendChangedBroadcast() {
        val active = database.openClashProfileDao().queryActiveProfile()

        context.sendBroadcastSelf(
            Intent(Intents.INTENT_ACTION_PROFILE_CHANGED)
                .putExtra(Intents.INTENT_EXTRA_PROFILE_ACTIVE, active)
        )
    }
}