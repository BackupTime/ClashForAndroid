package com.github.kr328.clash.settings

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.github.kr328.clash.R
import com.github.kr328.clash.preference.UiSettings

class InterfaceFragment: BaseSettingFragment() {
    companion object {
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_LANGUAGE = "language"
    }

    override fun onCreateDataStore(): SettingsDataStore {
        return SettingsDataStore().apply {
            on(KEY_DARK_MODE, DarkModeSource())
            on(KEY_LANGUAGE, UiSettings.LANGUAGE.asSource(ui))
        }
    }

    override val xmlResourceId: Int
        get() = R.xml.settings_interface

    private inner class DarkModeSource: SettingsDataStore.Source {
        override fun set(value: Any?) {
            ui.commit {
                put(UiSettings.DARK_MODE, value as String)
            }

            requireActivity().recreate()
        }

        override fun get(): Any? {
            return when ( activity.delegate.localNightMode ) {
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM ->
                    UiSettings.DARK_MODE_AUTO
                AppCompatDelegate.MODE_NIGHT_YES ->
                    UiSettings.DARK_MODE_DARK
                AppCompatDelegate.MODE_NIGHT_NO ->
                    UiSettings.DARK_MODE_LIGHT
                else -> UiSettings.DARK_MODE_AUTO
            }
        }

        private val activity: AppCompatActivity
            get() = requireActivity() as AppCompatActivity
    }
}