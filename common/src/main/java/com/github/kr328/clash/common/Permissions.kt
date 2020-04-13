package com.github.kr328.clash.common

object Permissions {
    val PERMISSION_ACCESS_CLASH: String
        get() = Global.application.packageName + ".permission.RECEIVE_BROADCASTS"
}