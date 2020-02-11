package com.github.kr328.clash.design.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.github.kr328.clash.design.R
import com.google.android.material.card.MaterialCardView

class TextCard @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attributeSet, defStyleAttr) {
    private val iconView: View
    private val titleView: TextView
    private val summaryView: TextView

    var title: CharSequence
        get() = titleView.text
        set(value) {
            titleView.text = value
        }

    var summary: CharSequence
        get() = summaryView.text
        set(value) {
            summaryView.text = value
        }

    var icon: Drawable?
        get() = iconView.background
        set(value) {
            iconView.background = value
        }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_text_card, this, true).apply {
            iconView = findViewById(android.R.id.icon)
            titleView = findViewById(android.R.id.title)
            summaryView = findViewById(android.R.id.summary)
        }

        // Custom attrs
        context.theme.obtainStyledAttributes(attributeSet, R.styleable.TextCard, 0, 0).apply {
            try {
                iconView.background = getDrawable(R.styleable.TextCard_icon)
                titleView.text = getString(R.styleable.TextCard_title)
                summaryView.text = getString(R.styleable.TextCard_summary)
            } finally {
                recycle()
            }
        }
    }
}