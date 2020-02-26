package com.github.kr328.clash.utils

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

const val DATE_DATE_ONLY = "yyyy-MM-dd"
const val DATE_TIME_ONLY = "HH:mm:ss"
const val DATE_ALL = "$DATE_DATE_ONLY $DATE_TIME_ONLY"

fun Date.format(
    context: Context,
    includeDate: Boolean = true,
    includeTime: Boolean = true,
    custom: String = ""
): String {
    val locale = context.resources.configuration.locales[0]

    return when {
        custom.isNotEmpty() ->
            SimpleDateFormat(custom, locale).format(this)
        includeDate && includeTime ->
            SimpleDateFormat(DATE_ALL, locale).format(this)
        includeDate ->
            SimpleDateFormat(DATE_DATE_ONLY, locale).format(this)
        includeTime ->
            SimpleDateFormat(DATE_TIME_ONLY, locale).format(this)
        else -> ""
    }
}