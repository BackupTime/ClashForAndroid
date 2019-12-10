package com.github.kr328.clash.view

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.widget.TextView

class MarqueeTextView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
): TextView(context, attributeSet, defStyleAttr, defStyleRes) {
    init {
        ellipsize = TextUtils.TruncateAt.MARQUEE
    }

    override fun isFocused(): Boolean {
        return true
    }
}