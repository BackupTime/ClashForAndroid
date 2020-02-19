package com.github.kr328.clash.settings

import android.os.Bundle
import com.github.kr328.clash.AccessControlPackagesActivity
import com.github.kr328.clash.BaseActivity
import com.github.kr328.clash.R
import com.github.kr328.clash.preference.UiSettings
import com.github.kr328.clash.remote.Broadcasts
import com.github.kr328.clash.service.settings.ServiceSettings
import com.github.kr328.clash.service.util.intent

class NetworkFragment: BaseSettingFragment() {
    companion object {
        private const val KEY_ENABLE_VPN_SERVICE = "enable_vpn_service"
        private const val KEY_IPV6 = "ipv6"
        private const val BYPASS_PRIVATE_NETWORK = "bypass_private_network"
        private const val KEY_DNS_HIJACKING = "dns_hijacking"
        private const val KEY_DNS_OVERRIDE = "dns_override"
        private const val KEY_APPEND_SYS_DNS = "append_system_dns"
        private const val KEY_ACCESS_CONTROL_MODE = "access_control_mode"
        private const val KEY_ACCESS_CONTROL_PACKAGES = "access_control_packages"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferenceScreen.isEnabled = !Broadcasts.clashRunning

        findPreference(KEY_ACCESS_CONTROL_PACKAGES).setOnPreferenceClickListener {
            startActivity(AccessControlPackagesActivity::class.intent)
            true
        }
    }

    override fun onCreateDataStore(): SettingsDataStore {
        return SettingsDataStore().apply {
            on(KEY_ENABLE_VPN_SERVICE, UiSettings.ENABLE_VPN.asSource(ui))
            on(KEY_IPV6, ServiceSettings.IPV6_SUPPORT.asSource(service))
            on(BYPASS_PRIVATE_NETWORK, ServiceSettings.BYPASS_PRIVATE_NETWORK.asSource(service))
            on(KEY_DNS_HIJACKING, ServiceSettings.DNS_HIJACKING.asSource(service))
            on(KEY_DNS_OVERRIDE, ServiceSettings.OVERRIDE_DNS.asSource(service))
            on(KEY_APPEND_SYS_DNS, ServiceSettings.AUTO_ADD_SYSTEM_DNS.asSource(service))
            on(KEY_ACCESS_CONTROL_MODE, ServiceSettings.ACCESS_CONTROL_MODE.asSource(service))
        }
    }

    override val xmlResourceId: Int
        get() = R.xml.settings_network
}

