package com.github.kr328.clash.design.settings

import android.content.Context
import android.graphics.drawable.Drawable

class SettingsBuilder(val screen: SettingsScreen) {
    val context: Context
        get() = screen.layout.context

    fun textInput(
        title: String = "",
        hint: CharSequence = "",
        icon: Drawable? = null,
        content: String = "",
        id: String? = null,
        dependOn: String? = null,
        setup: TextInput.() -> Unit = {}
    ) {
        val textInput = TextInput(screen)

        textInput.title = title
        textInput.hint = hint
        textInput.icon = icon
        textInput.content = content
        textInput.id = id
        textInput.dependOn = dependOn?.run { screen.requireElement(this) }

        setup(textInput)

        screen.addElement(textInput)
    }

    fun option(
        title: String = "",
        summary: String = "",
        icon: Drawable? = null,
        id: String? = null,
        dependOn: String? = null,
        setup: Option.() -> Unit = {}
    ) {
        val option = Option(screen)

        option.title = title
        option.summary = summary
        option.icon = icon
        option.id = id
        option.dependOn = dependOn?.run { screen.requireElement(this) }

        setup(option)

        screen.addElement(option)
    }
}