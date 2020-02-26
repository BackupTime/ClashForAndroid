package com.github.kr328.clash.service.net

import android.content.Context
import android.net.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.net.InetAddress

class DefaultNetworkChannel(val context: Context, scope: CoroutineScope) :
    CoroutineScope by scope,
    Channel<Pair<Network, LinkProperties?>?> by Channel(Channel.CONFLATED) {
    private val sendLock = Mutex()
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)!!

    private var currentNetwork: Network? = null

    private val callback = object : ConnectivityManager.NetworkCallback() {
        private val capabilitiesCache = mutableMapOf<Network, NetworkCapabilities?>()
        private val dnsServerCache = mutableMapOf<Network, List<InetAddress>>()

        override fun onAvailable(network: Network) {
            sendDefaultNetwork(true)
        }

        override fun onLost(network: Network) {
            sendDefaultNetwork(true)

            capabilitiesCache.remove(network)
            dnsServerCache.remove(network)
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities?
        ) {
            val cap = capabilitiesCache[network]

            if (cap?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                != networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            ) {
                sendDefaultNetwork(true)
            }

            capabilitiesCache[network] = networkCapabilities
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            val cache = dnsServerCache[network]

            if (cache != linkProperties.dnsServers) {
                sendDefaultNetwork(false)
            }

            dnsServerCache[network] = linkProperties.dnsServers
        }
    }

    fun register() {
        connectivity.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
    }

    fun unregister() {
        connectivity.unregisterNetworkCallback(callback)
    }

    private fun sendDefaultNetwork(ignoreSame: Boolean) {
        if (!sendLock.tryLock())
            return

        launch {
            val network = detectDefaultNetwork()
            val link = network?.let(connectivity::getLinkProperties)

            if (ignoreSame && network == currentNetwork)
                return@launch sendLock.unlock()

            currentNetwork = network

            if (network != null)
                send(network to link)
            else
                send(null)

            sendLock.unlock()
        }
    }

    private suspend fun detectDefaultNetwork() = withContext(Dispatchers.Default) {
        try {
            connectivity.allNetworks
                .asSequence()
                .mapNotNull { network ->
                    connectivity.getNetworkCapabilities(network)?.let { it to network }
                }
                .filterNot {
                    it.first.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
                            !it.first.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }
                .sortedBy {
                    when {
                        it.first.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> 0
                        it.first.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 1
                        it.first.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> 2
                        it.first.hasTransport(NetworkCapabilities.TRANSPORT_LOWPAN) -> 3
                        it.first.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> 4
                        else -> 5
                    } + if (it.first.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED))
                        -1000
                    else
                        0
                }
                .map {
                    it.second
                }
                .firstOrNull()
        } catch (e: Exception) {
            null
        }
    }
}