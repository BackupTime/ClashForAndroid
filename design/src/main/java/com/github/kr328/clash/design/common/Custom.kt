package com.github.kr328.clash.design.common

import android.os.Bundle
import android.view.View

class Custom(screen: CommonUiScreen, override val view: View): Base(screen) {
    private var saveStateHandler: (Bundle) -> Unit = {}
    private var restoreStateHandler: (Bundle) -> Unit = {}
    private var applyAttributeHandler: (Boolean, Boolean) -> Unit =
        { enable, hidden -> super.applyAttribute(enable, hidden)  }

    fun onSaveState(handler: (Bundle) -> Unit) {
        this.saveStateHandler = handler
    }

    fun onRestoreState(handler: (Bundle) -> Unit) {
        this.restoreStateHandler = handler
    }

    fun onApplyAttribute(handler: (Boolean, Boolean) -> Unit) {
        this.applyAttributeHandler = handler
    }

    override fun saveState(bundle: Bundle) {
        saveStateHandler(bundle)
    }

    override fun restoreState(bundle: Bundle) {
        restoreStateHandler(bundle)
    }
}