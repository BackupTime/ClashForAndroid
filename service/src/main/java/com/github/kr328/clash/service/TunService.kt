package com.github.kr328.clash.service

import android.content.Intent
import android.net.VpnService
import com.github.kr328.clash.service.clash.ClashRuntime
import com.github.kr328.clash.service.clash.module.*
import com.github.kr328.clash.service.settings.ServiceSettings
import com.github.kr328.clash.service.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class TunService : VpnService(), CoroutineScope by MainScope() {
    companion object {
        private const val TUN_MTU = 9000
        private const val TUN_DNS = "198.18.0.1"
        private const val IPV4_ANY = "0.0.0.0/0"
    }

    private val service = this
    private val runtime = ClashRuntime(this)
    private var reason: String? = null

    override fun onCreate() {
        super.onCreate()

        if (ServiceStatusProvider.serviceRunning)
            return stopSelf()

        ServiceStatusProvider.serviceRunning = true

        StaticNotificationModule.createNotificationChannel(this)
        StaticNotificationModule.notifyLoadingNotification(this)

        launch {
            val settings = ServiceSettings(service)
            val dnsInject = DnsInjectModule()

            runtime.install(TunModule(service)) {
                configure = TunConfigure(settings)
            }

            runtime.install(ReloadModule(service)) {
                onLoaded {
                    if (it != null) {
                        service.stopSelfForReason(it.message)
                    } else {
                        service.broadcastProfileLoaded()
                    }
                }
            }
            runtime.install(CloseModule()) {
                onClosed {
                    service.stopSelfForReason(null)
                }
            }

            if (settings.get(ServiceSettings.NOTIFICATION_REFRESH))
                runtime.install(DynamicNotificationModule(service))
            else
                runtime.install(StaticNotificationModule(service))

            runtime.install(dnsInject) {
                dnsOverride = settings.get(ServiceSettings.OVERRIDE_DNS)
            }

            runtime.install(NetworkObserveModule(service)) {
                onNetworkChanged { network, dnsServers ->
                    setUnderlyingNetworks(network?.let { arrayOf(it) })

                    if (settings.get(ServiceSettings.AUTO_ADD_SYSTEM_DNS)) {
                        val dnsStrings = dnsServers.map {
                            it.asSocketAddressText(53)
                        }

                        dnsInject.appendDns = dnsStrings
                    }

                    broadcastNetworkChanged()
                }
            }

            runtime.exec()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        service.broadcastClashStarted()

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        TunModule.requestStop()

        ServiceStatusProvider.serviceRunning = false

        service.broadcastClashStopped(reason)

        cancel()

        super.onDestroy()
    }

    private inner class TunConfigure(private val settings: ServiceSettings) : TunModule.Configure {
        private val network = generateTunNetwork()

        override val builder: Builder
            get() = Builder()
        override val mtu: Int
            get() = TUN_MTU
        override val gateway: String = network.copyOf().apply {
                this[3] = (this[3] + 1).toByte()
            }.asAddressString("/30")
        override val mirror: String = network.copyOf().apply {
                this[3] = (this[3] + 2).toByte()
            }.asAddressString()
        override val route: List<String>
            get() {
                return if (settings.get(ServiceSettings.BYPASS_PRIVATE_NETWORK))
                    resources.getStringArray(R.array.bypass_private_route).toList()
                else
                    listOf(IPV4_ANY)
            }
        override val dnsAddress: String
            get() = TUN_DNS
        override val dnsHijacking: Boolean
            get() = settings.get(ServiceSettings.DNS_HIJACKING)
        override val allowApplications: Collection<String>
            get() {
                return if (settings.get(ServiceSettings.ACCESS_CONTROL_MODE) == ServiceSettings.ACCESS_CONTROL_MODE_WHITELIST) {
                    (settings.get(ServiceSettings.ACCESS_CONTROL_PACKAGES) + packageName)
                } else emptySet()
            }
        override val disallowApplication: Collection<String>
            get() {
                return if (settings.get(ServiceSettings.ACCESS_CONTROL_MODE) == ServiceSettings.ACCESS_CONTROL_MODE_BLACKLIST) {
                    (settings.get(ServiceSettings.ACCESS_CONTROL_PACKAGES) - packageName)
                } else emptySet()
            }

        override fun onCreateTunFailure() {
            stopSelfForReason("Establish VPN rejected by system")
        }

        private fun generateTunNetwork(): ByteArray {
            val result = ByteArray(4)

            // 18 bit namespace
            val offset = UserUtils.currentUserId % 0x40000
            val network = (0x3FFFF - offset) shl 2

            result[0] = (172 or (network shr 24) and 0xFF).toByte()
            result[1] = (16 or (network shr 16) and 0xFF).toByte()
            result[2] = (0 or (network shr 8) and 0xFF).toByte()
            result[3] = (0 or (network shr 0) and 0xFF).toByte()

            return result
        }

        private fun ByteArray.asAddressString(suffix: String = ""): String {
            return "${this[0].toUnsignedString()}.${this[1].toUnsignedString()}.${this[2].toUnsignedString()}.${this[3].toUnsignedString()}$suffix"
        }

        private fun Byte.toUnsignedString(): String {
            return (this.toInt() and 0xFF).toString()
        }
    }

    private fun stopSelfForReason(reason: String?) {
        this.reason = reason

        stopSelf()

        TunModule.requestStop()
    }
}