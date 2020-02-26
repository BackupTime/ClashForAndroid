package com.github.kr328.clash.service.settings

import android.content.Context
import android.content.SharedPreferences
import com.github.kr328.clash.service.Constants
import rikka.preference.MultiProcessPreference

class ServiceSettings(preference: SharedPreferences) :
    BaseSettings(preference) {
    constructor(context: Context) : this(
        MultiProcessPreference(
            context,
            context.packageName + Constants.SETTING_PROVIDER_SUFFIX
        )
    )

    companion object {
        const val ACCESS_CONTROL_MODE_ALL = "access_control_mode_all"
        const val ACCESS_CONTROL_MODE_BLACKLIST = "access_control_mode_blacklist"
        const val ACCESS_CONTROL_MODE_WHITELIST = "access_control_mode_whitelist"

        val LANGUAGE =
            StringEntry("language", "")
        val IPV6_SUPPORT =
            BooleanEntry("ipv6_support", false)
        val BYPASS_PRIVATE_NETWORK =
            BooleanEntry("bypass_private_network", true)
        val ACCESS_CONTROL_MODE =
            StringEntry("access_control_mode", ACCESS_CONTROL_MODE_ALL)
        val ACCESS_CONTROL_PACKAGES =
            StringSetEntry("access_control_packages", emptySet())
        val DNS_HIJACKING =
            BooleanEntry("dns_hijacking", true)
        val NOTIFICATION_REFRESH =
            BooleanEntry("notification_refresh", true)
        val AUTO_ADD_SYSTEM_DNS =
            BooleanEntry("auto_add_system_dns", true)
        val OVERRIDE_DNS =
            BooleanEntry("override_dns", true)
    }
}