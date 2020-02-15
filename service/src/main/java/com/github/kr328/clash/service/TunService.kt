package com.github.kr328.clash.service

import android.net.VpnService
import android.os.Build
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.utils.Log
import com.github.kr328.clash.service.net.DefaultNetworkChannel
import kotlinx.coroutines.*

class TunService : VpnService(), CoroutineScope by MainScope() {
    companion object {
        // from https://github.com/shadowsocks/shadowsocks-android/blob/master/core/src/main/java/com/github/shadowsocks/bg/VpnService.kt
        private const val VPN_MTU = 1500
        private const val PRIVATE_VLAN4_SUBNET = 30
        private const val PRIVATE_VLAN4_CLIENT = "172.31.255.253"
        private const val PRIVATE_VLAN_DNS = "172.31.255.254"
        private const val VLAN4_ANY = "0.0.0.0"
    }

    private lateinit var defaultNetworkChannel: DefaultNetworkChannel
    private lateinit var settings: Settings

    private fun startTun() {
        val fd = Builder()
            .addAddress()
            .addDnsServer(PRIVATE_VLAN_DNS)
            .addBypassApplications()
            .addBypassPrivateRoute()
            .setMtu(VPN_MTU)
            .setBlocking(false)
            .setMeteredCompat(false)
            .establish()
            ?: throw NullPointerException("Unable to create VPN Service")

        val dnsAddress =
            if (settings.get(Settings.DNS_HIJACKING))
                "$VLAN4_ANY:53"
            else
                "$PRIVATE_VLAN_DNS:53"

        Log.i("TunService.startTun ${fd.fd}")

        Clash.startTunDevice(fd.fd, VPN_MTU, dnsAddress) {
            stopSelf()
        }

        fd.close()
    }

    override fun onCreate() {
        super.onCreate()

        Clash.initialize(this)

        settings = Settings(ClashManager(this, this))

        defaultNetworkChannel = DefaultNetworkChannel(this, this)

        defaultNetworkChannel.register()

        launch {
            withContext(Dispatchers.IO) {
                startTun()
            }

            while (isActive) {
                setUnderlyingNetworks(defaultNetworkChannel.receive()?.let { arrayOf(it) })
            }
        }
    }

    override fun onDestroy() {
        cancel()

        defaultNetworkChannel.unregister()

        Log.i("TunService.onDestroy")

        super.onDestroy()
    }

    private fun Builder.setMeteredCompat(isMetered: Boolean): Builder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            setMetered(isMetered)
        return this
    }

    private fun Builder.addBypassPrivateRoute(): Builder {
        // IPv4
        if (settings.get(Settings.BYPASS_PRIVATE_NETWORK)) {
            resources.getStringArray(R.array.bypass_private_route).forEach {
                val address = it.split("/")
                addRoute(address[0], address[1].toInt())
            }
        } else {
            addRoute("0.0.0.0", 0)
        }

        return this
    }

    private fun Builder.addBypassApplications(): Builder {
        when (settings.get(Settings.ACCESS_CONTROL_MODE)) {
            Settings.ACCESS_CONTROL_MODE_ALL -> {
                for (app in resources.getStringArray(R.array.default_disallow_application)) {
                    runCatching {
                        addDisallowedApplication(app)
                    }
                }
                addDisallowedApplication(packageName)
            }
            Settings.ACCESS_CONTROL_MODE_WHITELIST -> {
                for (app in settings.get(Settings.ACCESS_CONTROL_PACKAGES).toSet() -
                        resources.getStringArray(R.array.default_disallow_application) -
                        setOf(packageName)) {
                    runCatching {
                        addAllowedApplication(app)
                    }.onFailure {
                        Log.w("Package $app not found")
                    }
                }
            }
            Settings.ACCESS_CONTROL_MODE_BLACKLIST -> {
                for (app in settings.get(Settings.ACCESS_CONTROL_PACKAGES).toSet() +
                        resources.getStringArray(R.array.default_disallow_application)) {
                    runCatching {
                        addDisallowedApplication(app)
                    }.onFailure {
                        Log.w("Package $app not found")
                    }
                }
                addDisallowedApplication(packageName)
            }
            else -> throw IllegalArgumentException("Invalid mode")
        }

        return this
    }

    private fun Builder.addAddress(): Builder {
        addAddress(PRIVATE_VLAN4_CLIENT, PRIVATE_VLAN4_SUBNET)

        return this
    }
}