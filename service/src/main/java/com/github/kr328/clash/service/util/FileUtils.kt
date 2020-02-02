package com.github.kr328.clash.service.util

import android.content.Context
import com.github.kr328.clash.service.Constants
import java.io.File

val Context.profileDir: File
    get() = this.filesDir.resolve(Constants.PROFILES_DIR)
val Context.clashDir: File
    get() = this.filesDir.resolve(Constants.CLASH_DIR)