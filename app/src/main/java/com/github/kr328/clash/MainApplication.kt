package com.github.kr328.clash

import android.app.Application
import android.content.Context
import com.github.kr328.clash.core.Global
import com.github.kr328.clash.dump.LogcatDumper
import com.github.kr328.clash.remote.Broadcasts
import com.github.kr328.clash.remote.Remote
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.AbstractCrashesListener
import com.microsoft.appcenter.crashes.Crashes
import com.microsoft.appcenter.crashes.ingestion.models.ErrorAttachmentLog
import com.microsoft.appcenter.crashes.model.ErrorReport

@Suppress("unused")
class MainApplication : Application() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        Global.init(this)
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize AppCenter
        if (BuildConfig.APP_CENTER_KEY.isNotEmpty() && !BuildConfig.DEBUG) {
            AppCenter.start(
                this,
                BuildConfig.APP_CENTER_KEY,
                Analytics::class.java, Crashes::class.java
            )

            Crashes.setListener(object : AbstractCrashesListener() {
                override fun getErrorAttachments(report: ErrorReport?): MutableIterable<ErrorAttachmentLog> {
                    report ?: return mutableListOf()

                    if (!report.stackTrace.contains("DeadObjectException"))
                        return mutableListOf()

                    val logcat = LogcatDumper.dump().joinToString(separator = "\n")

                    return mutableListOf(
                        ErrorAttachmentLog.attachmentWithText(logcat, "logcat.txt")
                    )
                }
            })
        }

        Remote.init(this)
        Broadcasts.init(this)
    }
}