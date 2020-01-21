package com.github.kr328.clash.service

class Settings(private val clashManager: IClashManager) {
    companion object {
        const val ACCESS_CONTROL_MODE_ALL = "access_control_mode_all"
        const val ACCESS_CONTROL_MODE_BLACKLIST = "access_control_mode_blacklist"
        const val ACCESS_CONTROL_MODE_WHITELIST = "access_control_mode_whitelist"

        val BYPASS_PRIVATE_NETWORK = BooleanSetting("bypass_private_network", true)
        val ACCESS_CONTROL_MODE = StringSetting("access_control_mode", ACCESS_CONTROL_MODE_ALL)
        val ACCESS_CONTROL_PACKAGES = PackageListSetting("access_control_packages", emptyList())
        val DNS_HIJACKING = BooleanSetting("dns_hijacking", true)
    }

    fun put(key: String, value: String) {
        clashManager.putSetting(key, value)
    }

    fun get(key: String): String? {
        return clashManager.getSetting(key)
    }

    fun <T>get(setting: Setting<T>): T {
        return setting.parseValue(get(setting.key))
    }

    fun <T>put(setting: Setting<T>, value: T) {
        put(setting.key, setting.valueToString(value))
    }

    interface Setting<T> {
        val key: String
        fun parseValue(value: String?): T
        fun valueToString(value: T): String
    }

    class BooleanSetting(override val key: String, private val def: Boolean): Setting<Boolean> {
        override fun parseValue(value: String?): Boolean {
            val v = value ?: return def
            return v.toBoolean()
        }
        override fun valueToString(value: Boolean): String {
            return value.toString()
        }
    }

    class IntSetting(override val key: String, private val def: Int): Setting<Int> {
        override fun parseValue(value: String?): Int {
            val v = value ?: return def
            return v.toIntOrNull() ?: def
        }
        override fun valueToString(value: Int): String {
            return value.toString()
        }
    }

    class StringSetting(override val key: String, val def: String): Setting<String> {
        override fun parseValue(value: String?): String {
            return value ?: def
        }
        override fun valueToString(value: String): String {
            return value
        }
    }

    class PackageListSetting(override val key: String, private val def: List<String>): Setting<List<String>> {
        override fun parseValue(value: String?): List<String> {
            val v = value ?: return def

            return v.split(":")
        }
        override fun valueToString(value: List<String>): String {
            return value.joinToString(":")
        }
    }
}