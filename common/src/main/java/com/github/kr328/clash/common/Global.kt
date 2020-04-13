package com.github.kr328.clash.common

import android.app.Application

object Global {
    lateinit var application: Application
        private set

    fun init(application: Application) {
        Global.application = application
    }
}