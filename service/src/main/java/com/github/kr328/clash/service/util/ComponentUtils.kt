package com.github.kr328.clash.service.util

import android.content.ComponentName
import com.github.kr328.clash.core.Global
import kotlin.reflect.KClass

val KClass<*>.componentName: ComponentName
    get() = ComponentName.createRelative(Global.application, this.java.name)