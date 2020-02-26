package com.github.kr328.clash.design.common

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.github.kr328.clash.design.R

class TextInput(screen: CommonUiScreen) : Base(screen) {
    override val view: View = LayoutInflater.from(context).inflate(
        R.layout.view_setting_text_input,
        screen.layout,
        false
    )

    private val vIcon: View = view.findViewById(android.R.id.icon)
    private val vTitle: TextView = view.findViewById(android.R.id.title)
    private val vContent: View = view.findViewById(android.R.id.content)
    private val vText: TextView = view.findViewById(android.R.id.text1)

    var icon: Drawable?
        get() = vIcon.background
        set(value) {
            vIcon.background = value
        }
    var title: String
        get() = vTitle.text?.toString() ?: ""
        set(value) {
            vTitle.text = value
        }
    var content: CharSequence = ""
        set(value) {
            vText.text = displayContent(value)
            field = value

            textChanged.apply {
                textChanged = {}
                this(value)
                textChanged = this
            }
        }
    var hint: CharSequence
        get() = vText.hint ?: ""
        set(value) {
            vText.hint = value
        }

    private var openInput: () -> Unit = this::openDialogInput
    private var textChanged: (CharSequence) -> Unit = {}
    private var displayContent: (CharSequence) -> CharSequence = { it }

    init {
        vContent.setOnClickListener {
            openInput()
        }
    }

    fun onOpenInput(block: () -> Unit) {
        this.openInput = block
    }

    fun onTextChanged(block: (CharSequence) -> Unit) {
        this.textChanged = block
    }

    fun onDisplayContent(block: (CharSequence) -> CharSequence) {
        this.displayContent = block

        // Apply display content transform
        content = content
    }

    override fun saveState(bundle: Bundle) {
        if (id == null)
            return

        bundle.putCharSequence(id, content)
    }

    override fun restoreState(bundle: Bundle) {
        if (id == null)
            return

        bundle.getCharSequence(id)?.apply {
            content = this
        }
    }

    fun openDialogInput() {
        val v = LayoutInflater.from(context)
            .inflate(R.layout.dialog_input_text, screen.layout, false)
        val c: EditText = v.findViewById(android.R.id.text1)

        c.setText(content)
        c.hint = hint

        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(v)
            .setPositiveButton(R.string.ok) { _, _ -> content = c.text?.toString() ?: "" }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .show()
    }
}