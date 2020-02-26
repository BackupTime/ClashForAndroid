package com.github.kr328.clash.service

import android.content.Context
import android.content.SharedPreferences
import rikka.preference.PreferenceProvider

class ServiceSettingsProvider : PreferenceProvider() {
    override fun onCreatePreference(context: Context?): SharedPreferences {
        return context!!.getSharedPreferences(
            Constants.SERVICE_SETTING_FILE_NAME,
            Context.MODE_PRIVATE
        )
    }
}