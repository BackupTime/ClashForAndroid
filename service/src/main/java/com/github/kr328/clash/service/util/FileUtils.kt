package com.github.kr328.clash.service.util

import com.github.kr328.clash.core.Global
import com.github.kr328.clash.service.Constants
import java.io.File

fun resolveProfile(id: Long): File {
    return Global.application.filesDir.resolve(Constants.PROFILES_DIR).resolve("$id.yaml")
}

fun resolveBase(id: Long): File {
    return Global.application.filesDir.resolve(Constants.CLASH_DIR).resolve(id.toString())
}