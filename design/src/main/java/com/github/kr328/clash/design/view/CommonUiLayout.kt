package com.github.kr328.clash.design.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.github.kr328.clash.design.common.CommonUiBuilder
import com.github.kr328.clash.design.common.CommonUiScreen

class CommonUiLayout @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attributeSet, defStyleAttr) {
    val screen: CommonUiScreen = CommonUiScreen(this)

    init {
        orientation = VERTICAL
    }

    fun build(builder: CommonUiBuilder.() -> Unit) {
        screen.clear()

        CommonUiBuilder(screen).apply(builder)
    }
}