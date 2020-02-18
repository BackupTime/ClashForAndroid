// from https://github.com/shadowsocks/shadowsocks-android/blob/master/core/src/main/java/com/github/shadowsocks/net/DefaultNetworkListener.kt
package com.github.kr328.clash.service.net

import android.content.Context
import android.net.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex

class DefaultNetworkChannel(val context: Context, scope: CoroutineScope) :
    CoroutineScope by scope, Channel<Pair<Network, LinkProperties>?> by Channel(Channel.CONFLATED) {
    private var currentNetwork: Network? = null
    private val detectDelayLock = Mutex()
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)!!
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            sendDefaultNetwork(true)
        }

        override fun onLost(network: Network) {
            sendDefaultNetwork(true)
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            if ( network == currentNetwork &&
                !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) )
                sendDefaultNetwork(true)
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            if ( network == currentNetwork )
                sendDefaultNetwork(false)
        }
    }

    fun register() {
        connectivity.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
    }

    fun unregister() {
        connectivity.unregisterNetworkCallback(callback)
    }

    private fun sendDefaultNetwork(ignoreSameNetwork: Boolean) {
        if (detectDelayLock.tryLock()) {
            launch {
                delay(1000)

                val network = detectDefaultNetwork()
                if ( ignoreSameNetwork && network == currentNetwork )
                    return@launch

                currentNetwork = network

                val linkProperties = network?.let { connectivity.getLinkProperties(it) }

                if ( network != null && linkProperties != null )
                    send(network to linkProperties)
                else
                    send(null)

                detectDelayLock.unlock()
            }
        }
    }

    private suspend fun detectDefaultNetwork(): Network? = withContext(Dispatchers.Default) {
        return@withContext try {
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