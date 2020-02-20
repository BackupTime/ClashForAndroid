package com.github.kr328.clash.utils

import android.content.Context
import com.github.kr328.clash.R

object IntervalUtils {
    private const val MILLIS_SECOND = 1000L
    private const val MILLIS_MINUTE = MILLIS_SECOND * 60
    private const val MILLIS_HOUR = MILLIS_MINUTE * 60
    private const val MILLIS_DAY = MILLIS_HOUR * 24
    private const val MILLIS_MONTH = MILLIS_DAY * 30
    private const val MILLIS_YEAR = MILLIS_MONTH * 12

    fun intervalString(context: Context, interval: Long): String {
        val year = interval / MILLIS_YEAR
        val month = interval / MILLIS_MONTH
        val day = interval / MILLIS_DAY
        val hour = interval / MILLIS_HOUR
        val minute = interval / MILLIS_MINUTE

        System.currentTimeMillis()

        return when {
            year > 0 -> context.getString(R.string.format_years, year)
            month > 0 -> context.getString(R.string.format_months, month)
            day > 0 -> context.getString(R.string.format_days, day)
            hour > 0 -> context.getString(R.string.format_hours, hour)
            minute > 0 -> context.getString(R.string.format_minutes, minute)
            else -> context.getString(R.string.recently)
        }
    }
}