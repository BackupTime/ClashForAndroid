package com.github.kr328.clash.remote

import android.content.Context
import android.net.Uri
import com.github.kr328.clash.service.Constants
import com.github.kr328.clash.service.ServiceStatusProvider

object RemoteUtils {
    fun detectClashRunning(context: Context): Boolean {
        val authority = Uri.Builder()
            .scheme("content")
            .authority("${context.packageName}${Constants.STATUS_PROVIDER_SUFFIX}")
            .build()

        val pong = context.contentResolver.call(
            authority,
            ServiceStatusProvider.METHOD_PING_CLASH_SERVICE,
            null,
            null
        )

        return pong != null
    }
}