package com.github.kr328.clash.design.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.github.kr328.clash.design.R

class Category(screen: CommonUiScreen) : Base(screen) {
    override val view: View =
        LayoutInflater.from(context).inflate(R.layout.view_category, screen.layout, false)

    private val vText: TextView = view.findViewById(R.id.text)
    private val vTopSeparator: View = view.findViewById(R.id.topSeparator)
    private val vBottomSeparator: View = view.findViewById(R.id.bottomSeparator)

    var text: CharSequence
        get() = vText.text
        set(value) {
            vText.text = value
        }

    var showTopSeparator: Boolean
        get() = vTopSeparator.visibility == View.VISIBLE
        set(value) {
            vTopSeparator.visibility =
                if (value) View.VISIBLE else View.GONE
        }
    var showBottomSeparator: Boolean
        get() = vBottomSeparator.visibility == View.VISIBLE
        set(value) {
            vBottomSeparator.visibility =
                if (value) View.VISIBLE else View.GONE
        }

    override fun saveState(bundle: Bundle) {}
    override fun restoreState(bundle: Bundle) {}
}