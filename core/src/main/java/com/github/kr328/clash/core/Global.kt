package com.github.kr328.clash.core

import android.app.Application

object Global {
    lateinit var application: Application

    fun init(application: Application) {
        this.application = application
    }
}