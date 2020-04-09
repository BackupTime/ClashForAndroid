package com.github.kr328.clash.service.files

import android.content.Context
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import com.github.kr328.clash.service.R
import com.github.kr328.clash.service.util.resolveBase
import com.github.kr328.clash.service.util.resolveProfile
import java.io.FileNotFoundException
import java.lang.UnsupportedOperationException

class ProfileDirectoryResolver(private val context: Context) {
    companion object {
        const val FILE_NAME_CONFIG = "config.yaml"
        const val FILE_NAME_PROVIDER = "providers"
    }

    private val nextResolver = ProviderResolver(context)

    fun resolve(id: Long, paths: List<String>): VirtualFile {
        if (paths.size == 1) {
            return object : VirtualFile {
                override fun name(): String {
                    return when (paths[0]) {
                        FILE_NAME_CONFIG ->
                            context.getString(R.string.profile_yaml)
                        FILE_NAME_PROVIDER ->
                            context.getString(R.string.provider_files)
                        else ->
                            throw FileNotFoundException()
                    }
                }

                override fun lastModified(): Long {
                    return 0
                }

                override fun size(): Long {
                    return resolveProfile(id).length()
                }

                override fun mimeType(): String {
                    return when (paths[0]) {
                        FILE_NAME_CONFIG ->
                            "text/plain"
                        FILE_NAME_PROVIDER ->
                            DocumentsContract.Document.MIME_TYPE_DIR
                        else ->
                            throw FileNotFoundException()
                    }
                }

                override fun listFiles(): List<String> {
                    if (paths[0] == FILE_NAME_PROVIDER) {
                        return resolveBase(id).list()?.toList() ?: emptyList()
                    }

                    return emptyList()
                }

                override fun openFile(mode: Int): ParcelFileDescriptor {
                    return if (paths[0] == FILE_NAME_CONFIG)
                        ParcelFileDescriptor.open(resolveProfile(id), mode)
                    else
                        throw UnsupportedOperationException()
                }
            }
        } else if (paths.size == 2) {
            return nextResolver.resolve(id, paths[1])
        }

        throw FileNotFoundException()
    }
}