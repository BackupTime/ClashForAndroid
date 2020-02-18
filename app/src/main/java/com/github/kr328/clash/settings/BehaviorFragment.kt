package com.github.kr328.clash.settings

import android.content.pm.PackageManager
import com.github.kr328.clash.OnBootReceiver
import com.github.kr328.clash.R
import com.github.kr328.clash.service.settings.ServiceSettings
import com.github.kr328.clash.service.util.componentName

class BehaviorFragment: BaseSettingFragment() {
    companion object {
        private const val KEY_START_ON_BOOT = "start_on_boot"
        private const val KEY_SHOW_TRAFFIC = "show_traffic"
    }

    override fun onCreateDataStore(): SettingsDataStore {
        return SettingsDataStore().apply {
            on(KEY_START_ON_BOOT, StartOnBootSource())
            on(KEY_SHOW_TRAFFIC, ServiceSettings.NOTIFICATION_REFRESH.asSource(service))
        }
    }

    override val xmlResourceId: Int
        get() = R.xml.settings_behavior

    private inner class StartOnBootSource: SettingsDataStore.Source {
        override fun set(value: Any?) {
            val v = value as Boolean? ?: return

            val status = if ( v )
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED

            requireActivity().packageManager.setComponentEnabledSetting(
                OnBootReceiver::class.componentName,
                status,
                PackageManager.DONT_KILL_APP)
        }

        override fun get(): Any? {
            val status = requireActivity().packageManager
                .getComponentEnabledSetting(OnBootReceiver::class.componentName)

            return status == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
    }
}