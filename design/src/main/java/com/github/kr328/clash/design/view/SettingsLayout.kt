package com.github.kr328.clash.design.view

import android.animation.LayoutTransition
import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.github.kr328.clash.design.settings.SettingsBuilder
import com.github.kr328.clash.design.settings.SettingsScreen

class SettingsLayout @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attributeSet, defStyleAttr) {
    val screen: SettingsScreen = SettingsScreen(this)

    init {
        layoutTransition = LayoutTransition()
        orientation = VERTICAL
    }

    fun build(builder: SettingsBuilder.() -> Unit) {
        screen.clear()

        SettingsBuilder(screen).apply(builder)
    }
}