package com.github.kr328.clash.design.common

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.github.kr328.clash.design.R

class Option(screen: CommonUiScreen) : Base(screen) {
    override val view: View =
        LayoutInflater.from(context).inflate(R.layout.view_setting_option, screen.layout, false)

    private val vIcon: View = view.findViewById(android.R.id.icon)
    private val vTitle: TextView = view.findViewById(android.R.id.title)
    private val vSummary: TextView = view.findViewById(android.R.id.summary)
    private val vPadding: View = view.findViewById(R.id.heightPadding)

    private var click: () -> Unit = {}

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
    var summary: CharSequence
        get() = vSummary.text
        set(value) {
            vSummary.text = value
            if (value.isEmpty())
                vSummary.visibility = View.GONE
            else
                vSummary.visibility = View.VISIBLE
        }
    var textColor: Int
        get() = vTitle.textColors.defaultColor
        set(value) {
            vTitle.setTextColor(value)
            vSummary.setTextColor(value)
        }
    var paddingHeight: Boolean
        get() = vPadding.visibility != View.GONE
        set(value) {
            if (value)
                vPadding.visibility = View.INVISIBLE
            else
                vPadding.visibility = View.GONE
        }

    init {
        view.setOnClickListener {
            click()
        }
    }

    fun onClick(block: () -> Unit) {
        this.click = block
    }

    override fun saveState(bundle: Bundle) {}
    override fun restoreState(bundle: Bundle) {}
}