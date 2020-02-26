package com.github.kr328.clash.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.kr328.clash.preference.UiSettings
import com.github.kr328.clash.service.settings.ServiceSettings
import moe.shizuku.preference.PreferenceFragment

abstract class BaseSettingFragment : PreferenceFragment() {
    abstract fun onCreateDataStore(): SettingsDataStore
    abstract val xmlResourceId: Int

    protected val service: ServiceSettings by lazy { ServiceSettings(requireActivity()) }
    protected val ui: UiSettings by lazy { UiSettings(requireActivity()) }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = onCreateDataStore()

        setPreferencesFromResource(xmlResourceId, rootKey)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val result = super.onCreateView(inflater, container, savedInstanceState)

        setDivider(null)
        setDividerHeight(0)

        return result
    }
}