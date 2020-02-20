package com.github.kr328.clash.core.utils

import com.github.kr328.clash.core.Constants.TAG

object Log {
    fun i(message: String, throwable: Throwable? = null) =
        android.util.Log.i(TAG, message, throwable)

    fun w(message: String, throwable: Throwable? = null) =
        android.util.Log.w(TAG, message, throwable)

    fun e(message: String, throwable: Throwable? = null) =
        android.util.Log.e(TAG, message, throwable)

    fun d(message: String, throwable: Throwable? = null) =
        android.util.Log.d(TAG, message, throwable)

    fun v(message: String, throwable: Throwable? = null) =
        android.util.Log.v(TAG, message, throwable)
}
