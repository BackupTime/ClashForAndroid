package com.github.kr328.clash.service

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import com.github.kr328.clash.service.util.resolveProfileFile
import com.github.kr328.clash.service.util.resolveTempProfileFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

class ProfileProvider : FileProvider() {
    companion object {
        private const val METHOD_ACQUIRE_TEMP = "acquireTemp"
        private const val METHOD_RELEASE_TEMP = "releaseTemp"

        // service side functions
        fun resolveUri(context: Context, file: File): Uri {
            return getUriForFile(
                context,
                context.packageName + Constants.PROFILE_PROVIDER_SUFFIX,
                file
            )
        }

        // client side functions
        suspend fun acquireTemp(context: Context, id: Long): Uri = withContext(Dispatchers.IO) {
            val uri = Uri.parse(context.packageName + Constants.PROFILE_PROVIDER_SUFFIX)

            context.contentResolver
                .call(uri, METHOD_ACQUIRE_TEMP, id.toString(), null)?.getParcelable<Uri>("uri")
                ?: throw FileNotFoundException("No such profile")
        }

        suspend fun releaseTemp(context: Context, id: Long) = withContext(Dispatchers.IO) {
            val uri = Uri.parse(context.packageName + Constants.PROFILE_PROVIDER_SUFFIX)

            context.contentResolver
                .call(uri, METHOD_RELEASE_TEMP, id.toString(), null)
        }
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        when (method) {
            METHOD_ACQUIRE_TEMP -> {
                val id = arg?.toLongOrNull() ?: return null

                val file = context!!.resolveProfileFile(id)
                if (!file.exists())
                    return null

                val temp = context!!.resolveTempProfileFile(id)
                if (!temp.exists())
                    file.copyTo(context!!.resolveTempProfileFile(id))

                return bundleOf("uri" to resolveUri(context!!, temp))
            }
            METHOD_RELEASE_TEMP -> {
                val id = arg?.toLongOrNull() ?: return null

                context!!.resolveTempProfileFile(id).delete()
            }
        }
        return super.call(method, arg, extras)
    }
}