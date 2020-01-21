package com.github.kr328.clash.service

import android.content.Context
import android.net.Uri
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.service.data.ClashDatabase
import com.github.kr328.clash.service.data.ClashProfileEntity
import com.github.kr328.clash.service.ipc.ParcelableCompletedFuture
import com.github.kr328.clash.service.util.DefaultThreadPool
import com.github.kr328.clash.service.util.FileUtils

class ClashProfileManager(private val context: Context, private val database: ClashDatabase) :
    IClashProfileManager.Stub() {
    private val clashDir = context.filesDir.resolve(Constants.CLASH_DIR)
    private val profileDir = context.filesDir.resolve(Constants.PROFILES_DIR)

    override fun updateProfile(id: Int): ParcelableCompletedFuture {
        val entity = database.openClashProfileDao().queryProfileById(id)

        require(
            entity != null && (entity.type == ClashProfileEntity.Type.URL ||
                    entity.type == ClashProfileEntity.Type.FILE)
        )

        val result = ParcelableCompletedFuture()

        downloadProfile(Uri.parse(entity.uri), entity.file, entity.base,
            onSuccess = {
                result.complete(null)

                database.openClashProfileDao().touchProfile(id)
            },
            onFailure = {
                result.completeExceptionally(it)
            })

        return result
    }

    override fun queryAllProfiles(): Array<ClashProfileEntity> {
        return database.openClashProfileDao().queryProfiles()
    }

    override fun addProfile(name: String, type: Int, uri: String?): ParcelableCompletedFuture {
        require(uri != null && (uri.startsWith("http") || uri.startsWith("content")))

        val result = ParcelableCompletedFuture()
        val fileName = FileUtils.generateRandomFileName(profileDir, ".yaml")
        val baseDirName = FileUtils.generateRandomFileName(clashDir)

        downloadProfile(Uri.parse(uri), fileName, baseDirName,
            onSuccess = {
                database.openClashProfileDao().addProfile(
                    ClashProfileEntity(
                        name,
                        ClashProfileEntity.intToType(type),
                        uri,
                        fileName,
                        baseDirName,
                        false,
                        System.currentTimeMillis()
                    )
                )

                result.complete(null)
            },
            onFailure = {
                result.completeExceptionally(it)
            })

        return result
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
}