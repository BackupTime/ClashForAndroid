package com.github.kr328.clash.design.common

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.github.kr328.clash.design.R

class Tips(screen: CommonUiScreen) : Base(screen) {
    override val view: View = LayoutInflater.from(context)
        .inflate(R.layout.view_setting_tip, screen.layout, false)

    private val vIcon: View = view.findViewById(android.R.id.icon)
    private val vTitle: TextView = view.findViewById(android.R.id.title)

    var icon: Drawable?
        get() = vIcon.background
        set(value) {
            vIcon.background = value
        }

    var title: CharSequence
        get() = vTitle.text
        set(value) {
            vTitle.text = value
        }

    override fun saveState(bundle: Bundle) {}
    override fun restoreState(bundle: Bundle) {}
}