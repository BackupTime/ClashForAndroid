package com.github.kr328.clash.core.event

import android.os.Parcelable

abstract class EventStream<T: Event> {
    private var on: (T) -> Unit = {}

    fun onEvent(callback: (T) -> Unit) {
        on = callback
    }

    fun close() {
        onClose()
    }

    fun send(event: T) {
        on(event)
    }

    abstract fun onClose()
}