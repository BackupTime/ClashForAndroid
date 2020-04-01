package com.github.kr328.clash.service

import android.net.VpnService
import android.os.Build
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.utils.Log
import com.github.kr328.clash.service.net.DefaultNetworkChannel
import com.github.kr328.clash.service.settings.ServiceSettings
import com.github.kr328.clash.service.util.asSocketAddressText
import com.github.kr328.clash.service.util.broadcastNetworkChanged
import kotlinx.coroutines.*

class TunService : VpnService(), CoroutineScope by MainScope() {
    companion object {
        // from https://github.com/shadowsocks/shadowsocks-android/blob/master/core/src/main/java/com/github/shadowsocks/bg/VpnService.kt
        private const val VPN_MTU = 9000
        private const val PRIVATE_VLAN4_SUBNET = 30
        private const val PRIVATE_VLAN6_SUBNET = 126
        private const val PRIVATE_VLAN4_CLIENT = "172.31.255.253"
        private const val PRIVATE_VLAN6_CLIENT = "fdfe:dcba:9876::1"
        private const val PRIVATE_VLAN4_MIRROR = "172.31.255.254"
        private const val PRIVATE_VLAN6_MIRROR = "fdfe:dcba:9876::2"
        private const val PRIVATE_VLAN_DNS = "198.18.0.1"
        private const val VLAN4_ANY = "0.0.0.0"
    }

    private var clashCore: ClashCore? = null

    private lateinit var defaultNetworkChannel: DefaultNetworkChannel
    private lateinit var settings: ServiceSettings

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
            if (settings.get(ServiceSettings.DNS_HIJACKING)) VLAN4_ANY else PRIVATE_VLAN_DNS

        Log.i("TunService.startTun ${fd.fd}")

        Clash.setDnsOverrideEnabled(settings.get(ServiceSettings.OVERRIDE_DNS))

        Clash.startTunDevice(
            fd.detachFd(), VPN_MTU,
            "$PRIVATE_VLAN4_CLIENT/$PRIVATE_VLAN4_SUBNET", PRIVATE_VLAN4_MIRROR,
            dnsAddress,
            this::protect, this::stopSelf
        )

        fd.close()
    }

    override fun onCreate() {
        super.onCreate()

        Clash.initialize(this)

        if (ServiceStatusProvider.serviceRunning)
            return stopSelf()

        clashCore = ClashCore(this)

        clashCore?.start()

        settings = ServiceSettings(this)

        defaultNetworkChannel = DefaultNetworkChannel(this, this)

        defaultNetworkChannel.register()

        launch {
            withContext(Dispatchers.IO) {
                startTun()
            }

            while (isActive) {
                val d = defaultNetworkChannel.receive()

                Log.i("Network changed to ${d?.second}")

                if (d == null) {
                    setUnderlyingNetworks(null)
                    continue
                }

                setUnderlyingNetworks(arrayOf(d.first))

                if (settings.get(ServiceSettings.AUTO_ADD_SYSTEM_DNS)) {
                    withContext(Dispatchers.Default) {
                        val dnsServers = d.second?.dnsServers ?: emptyList()

                        val dnsStrings = dnsServers.map {
                            it.asSocketAddressText(53)
                        }

                        Clash.appendDns(dnsStrings)
                    }
                }

                broadcastNetworkChanged(this@TunService)
            }
        }
    }

    override fun onDestroy() {
        cancel()

        clashCore?.apply {
            destroy()

            defaultNetworkChannel.unregister()
        }

        Log.i("TunService.onDestroy")

        super.onDestroy()
    }

    private fun Builder.setMeteredCompat(isMetered: Boolean): Builder {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            setMetered(isMetered)
        return this
    }

    private fun Builder.addBypassPrivateRoute(): Builder {
        val ipv6Support = settings.get(ServiceSettings.IPV6_SUPPORT)
        val bypassPrivate = settings.get(ServiceSettings.BYPASS_PRIVATE_NETWORK)

        // IPv4
        if (bypassPrivate) {
            resources.getStringArray(R.array.bypass_private_route).forEach {
                val address = it.split("/")
                addRoute(address[0], address[1].toInt())
            }
        } else {
            addRoute("0.0.0.0", 0)
        }

        // IPv6
        if (ipv6Support) {
            if (bypassPrivate)
            // from https://github.com/shadowsocks/shadowsocks-android/commit/cc840c9fddb3f4f6677005de18f1fcb387b84064#diff-e089fe63dcb3674c0a1e459a95508e3e
                addRoute("2000::", 3)
            else
                addRoute("::", 0)
        }

        return this
    }

    private fun Builder.addBypassApplications(): Builder {
        when (settings.get(ServiceSettings.ACCESS_CONTROL_MODE)) {
            ServiceSettings.ACCESS_CONTROL_MODE_ALL -> {
            }
            ServiceSettings.ACCESS_CONTROL_MODE_WHITELIST -> {
                for (app in settings.get(ServiceSettings.ACCESS_CONTROL_PACKAGES).toSet()) {
                    runCatching {
                        addAllowedApplication(app)
                    }.onFailure {
                        Log.w("Package $app not found")
                    }
                }
            }
            ServiceSettings.ACCESS_CONTROL_MODE_BLACKLIST -> {
                for (app in settings.get(ServiceSettings.ACCESS_CONTROL_PACKAGES).toSet()) {
                    runCatching {
                        addDisallowedApplication(app)
                    }.onFailure {
                        Log.w("Package $app not found")
                    }
                }
            }
            else -> throw IllegalArgumentException("Invalid mode")
        }

        return this
    }

    private fun Builder.addAddress(): Builder {
        addAddress(PRIVATE_VLAN4_CLIENT, PRIVATE_VLAN4_SUBNET)

        if (settings.get(ServiceSettings.IPV6_SUPPORT))
            addAddress(PRIVATE_VLAN6_CLIENT, PRIVATE_VLAN6_SUBNET)

        return this
    }
}