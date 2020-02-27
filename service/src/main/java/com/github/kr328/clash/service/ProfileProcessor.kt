package com.github.kr328.clash.service

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.service.data.ClashDatabase
import com.github.kr328.clash.service.data.ClashProfileEntity
import com.github.kr328.clash.service.util.resolveBase
import com.github.kr328.clash.service.util.resolveProfile
import java.io.File
import java.io.FileNotFoundException

class ProfileProcessor(private val context: Context) {
    suspend fun createOrUpdate(entity: ClashProfileEntity, newRecord: Boolean) {
        val database = ClashDatabase.getInstance(context).openClashProfileDao()

        val uri = Uri.parse(entity.uri)
        if (uri == null || uri == Uri.EMPTY)
            throw IllegalArgumentException("Invalid uri $uri")

        downloadProfile(
            uri,
            resolveProfile(entity.id),
            resolveBase(entity.id),
            newRecord
        )

        val newEntity = if (entity.type == ClashProfileEntity.TYPE_FILE)
            entity.copy(
                lastUpdate = System.currentTimeMillis(),
                uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}${Constants.PROFILE_PROVIDER_SUFFIX}",
                    resolveProfile(entity.id)
                ).toString()
            )
        else
            entity.copy(lastUpdate = System.currentTimeMillis())

        if (newRecord)
            database.addProfile(newEntity)
        else
            database.updateProfile(newEntity)
    }

    suspend fun remove(id: Long) {
        val database = ClashDatabase.getInstance(context).openClashProfileDao()

        resolveProfile(id).delete()
        resolveBase(id).deleteRecursively()

        database.removeProfile(id)
    }

    fun clear(id: Long) {
        resolveBase(id).listFiles()?.forEach {
            it.deleteRecursively()
        }
    }

    private suspend fun downloadProfile(source: Uri, target: File, baseDir: File, newRecord: Boolean) {
        try {
            target.parentFile?.mkdirs()
            baseDir.mkdirs()

            if (source.scheme.equals("content", ignoreCase = true)
                || source.scheme.equals("file", ignoreCase = true)) {
                val parcelFileDescriptor = context.contentResolver.openFileDescriptor(source, "r")
                    ?: throw FileNotFoundException("Unable to open file $source")

                val fd = parcelFileDescriptor.detachFd()

                Clash.downloadProfile(fd, target, baseDir).await()
            } else {
                Clash.downloadProfile(source.toString(), target, baseDir).await()
            }
        } catch (e: Exception) {
            if ( newRecord ) {
                target.delete()
                baseDir.deleteRecursively()
            }
            throw e
        }
    }
}