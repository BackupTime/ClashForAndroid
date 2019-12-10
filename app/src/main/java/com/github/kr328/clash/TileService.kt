package com.github.kr328.clash

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.github.kr328.clash.core.event.ProcessEvent
import com.github.kr328.clash.core.event.ProfileReloadEvent
import com.github.kr328.clash.service.ClashService
import com.github.kr328.clash.service.Constants
import com.github.kr328.clash.service.IClashEventObserver
import com.github.kr328.clash.service.IClashService
import com.github.kr328.clash.service.data.ClashProfileEntity
import com.github.kr328.clash.utils.ServiceUtils

class TileService : TileService() {
    override fun onClick() {
        val tile = qsTile

        when (tile.state) {
            Tile.STATE_INACTIVE -> {
                ServiceUtils.startStarterService(this)
            }
            Tile.STATE_ACTIVE -> {
                val binder =
                    clashStatusReceiver.peekService(this, Intent(this, ClashService::class.java))

                runCatching {
                    val clash = IClashService.Stub.asInterface(binder)

                    clash?.stop()
                }
            }
        }
    }

    override fun onStartListening() {
        refreshStatus()
    }

    private fun refreshStatus() {
        if ( qsTile == null )
            return

        val current = getCurrentStatus()

        when (current.first) {
            ProcessEvent.STARTED -> {
                qsTile.state = Tile.STATE_ACTIVE
                qsTile.label = current.second?.name ?: getString(R.string.launch_name)
            }
            ProcessEvent.STOPPED -> {
                qsTile.state = Tile.STATE_INACTIVE
                qsTile.label = getString(R.string.launch_name)
            }
        }

        qsTile.updateTile()
    }

    private val clashStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshStatus()
        }
    }

    override fun onCreate() {
        super.onCreate()

        registerReceiver(
            clashStatusReceiver,
            IntentFilter().apply {
                addAction(Constants.CLASH_PROCESS_BROADCAST_ACTION)
                addAction(Constants.CLASH_RELOAD_BROADCAST_ACTION)
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(clashStatusReceiver)
    }

    private fun getCurrentStatus(): Pair<ProcessEvent, ClashProfileEntity?> {
        val service =
            IClashService.Stub.asInterface(clashStatusReceiver
                .peekService(this, Intent(this, ClashService::class.java)))

        return runCatching {
            (service?.currentProcessStatus ?: ProcessEvent.STOPPED) to service?.profileService?.queryActiveProfile()
        }.getOrNull() ?: ProcessEvent.STOPPED to null
    }
}