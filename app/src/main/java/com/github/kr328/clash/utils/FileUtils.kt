package com.github.kr328.clash.utils

import android.content.Context
import com.github.kr328.clash.Constants
import java.io.File

val Context.logsDir: File
    get() = (externalCacheDir ?: cacheDir).resolve(Constants.LOG_DIR_NAME)