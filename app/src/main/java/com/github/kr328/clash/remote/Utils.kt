package com.github.kr328.clash.remote

import android.os.DeadObjectException
import com.github.kr328.clash.dump.LogcatDumper
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog

internal object Utils {
    fun <T>callRemote(block: () -> T): T {
        return try {
            block()
        }
        catch (e: DeadObjectException) {
            val log = LogcatDumper.dump().joinToString(separator = "\n")

            val attachmentLog = ErrorAttachmentLog
                .attachmentWithText(log, "logcat.txt")

            Crashes.trackError(e, null, listOf(attachmentLog))

            throw e
        }
    }
}