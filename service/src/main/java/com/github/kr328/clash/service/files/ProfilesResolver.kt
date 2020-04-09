package com.github.kr328.clash.service.files

import android.content.Context
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import com.github.kr328.clash.service.R
import com.github.kr328.clash.service.data.ClashDatabase
import com.github.kr328.clash.service.data.ClashProfileEntity
import java.io.FileNotFoundException
import java.lang.UnsupportedOperationException

class ProfilesResolver(private val context: Context, private val database: ClashDatabase) {
    private val nextResolver = ProfileDirectoryResolver(context)

    suspend fun resolve(paths: List<String>): VirtualFile {
        return when (paths.size) {
            0 -> {
                val files = database.openClashProfileDao()
                    .queryProfiles()
                    .map(ClashProfileEntity::id)
                    .map(Long::toString)

                object : VirtualFile {
                    override fun name(): String {
                        return context.getString(R.string.clash_for_android)
                    }

                    override fun lastModified(): Long {
                        return 0
                    }

                    override fun size(): Long {
                        return 0
                    }

                    override fun mimeType(): String {
                        return DocumentsContract.Document.MIME_TYPE_DIR
                    }

                    override fun listFiles(): List<String> {
                        return files
                    }

                    override fun openFile(mode: Int): ParcelFileDescriptor {
                        throw UnsupportedOperationException()
                    }
                }
            }
            1 -> {
                val profile = paths[0].toLongOrNull()?.let {
                    database.openClashProfileDao().queryProfileById(it)
                } ?: throw FileNotFoundException()

                object : VirtualFile {
                    override fun name(): String {
                        return profile.name
                    }

                    override fun lastModified(): Long {
                        return profile.lastUpdate
                    }

                    override fun size(): Long {
                        return 0
                    }

                    override fun mimeType(): String {
                        return DocumentsContract.Document.MIME_TYPE_DIR
                    }

                    override fun listFiles(): List<String> {
                        return listOf(
                            ProfileDirectoryResolver.FILE_NAME_CONFIG,
                            ProfileDirectoryResolver.FILE_NAME_PROVIDER
                        )
                    }

                    override fun openFile(mode: Int): ParcelFileDescriptor {
                        throw UnsupportedOperationException()
                    }
                }
            }
            else -> {
                val id = paths[0].toLongOrNull() ?: throw FileNotFoundException()

                nextResolver.resolve(id, paths.subList(1, paths.size))
            }
        }
    }
}