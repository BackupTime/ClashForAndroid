// from https://github.com/shadowsocks/shadowsocks-android/blob/master/core/src/main/java/com/github/shadowsocks/net/DefaultNetworkListener.kt
package com.github.kr328.clash.service.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.github.kr328.clash.core.utils.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DefaultNetworkChannel(val context: Context, scope: CoroutineScope):
    CoroutineScope by scope, Channel<Network?> by Channel(Channel.CONFLATED) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)!!
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            launch {
                send(rebuildNetworkList())
            }
        }
        override fun onLost(network: Network) {
            launch {
                send(rebuildNetworkList())
            }
        }
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            launch {
                send(rebuildNetworkList())
            }
        }
    }

    fun register() {
        connectivity.registerNetworkCallback(NetworkRequest.Builder().build(), callback)
    }

    fun unregister() {
        connectivity.unregisterNetworkCallback(callback)
    }

    private suspend fun rebuildNetworkList(): Network? = withContext(Dispatchers.Default) {
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
                    Log.i("Network ${it.first}")
                    it
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