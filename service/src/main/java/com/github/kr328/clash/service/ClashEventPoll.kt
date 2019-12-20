package com.github.kr328.clash.service

import android.os.Handler
import com.github.kr328.clash.core.Clash
import com.github.kr328.clash.core.event.BandwidthEvent
import com.github.kr328.clash.core.event.LogEvent
import com.github.kr328.clash.core.event.TrafficEvent

class ClashEventPoll(private val clash: Clash, private val master: Master) {
    interface Master {
        fun onLogEvent(event: LogEvent)
        fun onTrafficEvent(event: TrafficEvent)
        fun onBandwidthEvent(event: BandwidthEvent)
    }

    private val handler = Handler()

    private var traffic: Clash.Poll? = null
    private var bandwidth: Clash.Poll? = null
    private var logs: Clash.Poll? = null

    fun startTrafficPoll() {
        handler.post {
            if ( traffic != null )
                return@post

            traffic = clash.pollTraffic {
                master.onTrafficEvent(it)
            }
        }
    }

    fun stopTrafficPoll() {
        handler.post {
            traffic?.stop()

            traffic = null
        }
    }

    fun startBandwidthPoll() {
        handler.post {
            if ( bandwidth != null )
                return@post

            bandwidth = clash.pollBandwidth {
                master.onBandwidthEvent(it)
            }
        }
    }

    fun stopBandwidthPoll() {
        handler.post {
            bandwidth?.stop()

            bandwidth = null
        }
    }

    fun startLogsPoll() {
        handler.post {
            if ( logs != null )
                return@post

            logs = clash.pollLogs {
                master.onLogEvent(it)
            }
        }
    }

    fun stopLogPoll() {
        handler.post {
            logs?.stop()

            logs = null
        }
    }

    fun shutdown() {
        stopTrafficPoll()
        stopBandwidthPoll()
        stopLogPoll()
    }
}