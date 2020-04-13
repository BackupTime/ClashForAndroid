package com.github.kr328.clash.service

import android.content.Context
import android.net.Uri
import android.webkit.URLUtil
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.service.data.ProfileDao
import com.github.kr328.clash.service.model.ProfileMetadata
import com.github.kr328.clash.service.model.ProfileMetadata.Type
import com.github.kr328.clash.service.model.toProfileEntity
import com.github.kr328.clash.service.util.resolveBaseDir
import com.github.kr328.clash.service.util.resolveProfileFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.util.*

object ProfileProcessor {
    suspend fun createOrUpdate(context: Context, metadata: ProfileMetadata) =
        withContext(Dispatchers.IO) {
            metadata.enforceFieldValid()

            downloadProfile(
                context, metadata.uri,
                context.resolveProfileFile(metadata.id),
                context.resolveBaseDir(metadata.id)
            )

            val entity = metadata.toProfileEntity()

            if (ProfileDao.queryById(metadata.id) == null)
                ProfileDao.insert(entity)
            else
                ProfileDao.update(entity)
        }

    private suspend fun downloadProfile(
        context: Context,
        source: Uri,
        target: File,
        baseDir: File
    ) = withContext(Dispatchers.IO) {
        when (source.scheme?.toLowerCase(Locale.getDefault())) {
            "http", "https" ->
                Clash.downloadProfile(source.toString(), target, baseDir)
            "content", "file", "resource" -> {
                val fd = context.contentResolver.openFileDescriptor(source, "r")
                    ?: throw FileNotFoundException("$source not found")
                Clash.downloadProfile(fd.detachFd(), target, baseDir)
            }
            else -> throw IllegalArgumentException("Invalid uri type")
        }.await()
    }

    private fun ProfileMetadata.enforceFieldValid() {
        when {
            id < 0 ->
                throw IllegalArgumentException("Invalid id")
            name.isBlank() ->
                throw IllegalArgumentException("Empty name")
            type != Type.FILE && type != Type.URL && type != Type.EXTERNAL ->
                throw IllegalArgumentException("Invalid type")
            !URLUtil.isValidUrl(uri.toString()) ->
                throw IllegalArgumentException("Invalid uri")
            source?.let { URLUtil.isValidUrl(it.toString()) } == false ->
                throw IllegalArgumentException("Invalid source")
            interval < 0 ->
                throw IllegalArgumentException("Invalid interval")
        }
    }
}