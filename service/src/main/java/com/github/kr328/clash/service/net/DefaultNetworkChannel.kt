package com.github.kr328.clash.service.net

import android.content.Context
import android.net.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex

class DefaultNetworkChannel(val context: Context, scope: CoroutineScope) :
    CoroutineScope by scope,
    Channel<Pair<Network, LinkProperties?>?> by Channel(Channel.CONFLATED) {
    private val sendLock = Mutex()
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)!!
    private val callback = object : ConnectivityManager.NetworkCallback() {
        private val capabilitiesCache = mutableMapOf<Network, NetworkCapabilities?>()

        override fun onAvailable(network: Network) {
            sendDefaultNetwork()
        }

        override fun onLost(network: Network) {
            sendDefaultNetwork()

            capabilitiesCache.remove(network)
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities?
        ) {
            val cap = capabilitiesCache[network]

            if (cap?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                != networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            )
                sendDefaultNetwork()

            capabilitiesCache[network] = networkCapabilities
        }
    }

    fun register() {
        connectivity.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
    }

    fun unregister() {
        connectivity.unregisterNetworkCallback(callback)
    }

    private fun sendDefaultNetwork() {
        if (!sendLock.tryLock())
            return

        launch {
            delay(1000)

            val network = detectDefaultNetwork()
            val link = network?.let(connectivity::getLinkProperties)

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