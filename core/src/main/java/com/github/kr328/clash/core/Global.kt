package com.github.kr328.clash.core

import android.app.Application
import android.content.res.Configuration

object Global {
    lateinit var application: Application
    val overrideConfiguration = Configuration()

    fun init(application: Application) {
        this.application = application
    }
}