package com.github.kr328.clash.design.common

import android.os.Bundle
import android.os.Handler
import com.github.kr328.clash.design.view.CommonUiLayout

class CommonUiScreen(val layout: CommonUiLayout) {
    private val handler = Handler()
    val elements = mutableListOf<Base>()

    fun clear() {
        elements.clear()
        layout.removeAllViews()
    }

    inline fun <reified T : Base> getElement(id: String): T? {
        return elements.singleOrNull { it.id == id } as T?
    }

    inline fun <reified T : Base> requireElement(id: String): T {
        return requireNotNull(getElement(id))
    }

    fun addElement(element: Base) {
        layout.addView(element.view)
        elements.add(element)
    }

    fun postReapplyAttribute() {
        handler.post {
            elements.forEach {
                it.reapplyAttribute()
            }
        }
    }

    fun saveState(bundle: Bundle) {
        elements.forEach {
            it.saveState(bundle)
        }
    }

    fun restoreState(bundle: Bundle?) {
        if (bundle == null)
            return

        elements.forEach {
            it.restoreState(bundle)
        }
    }
}