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
            on(KEY_DARK_MODE, UiSettings.DARK_MODE.asSource(ui))
            on(KEY_LANGUAGE, UiSettings.LANGUAGE.asSource(ui))

            onApply {
                requireActivity().recreate()
            }
        }
    }

    override val xmlResourceId: Int
        get() = R.xml.settings_interface
}