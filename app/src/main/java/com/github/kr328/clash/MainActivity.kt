package com.github.kr328.clash

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.view.View
import com.github.kr328.clash.core.utils.asBytesString
import com.github.kr328.clash.remote.withClash
import com.github.kr328.clash.service.ClashService
import com.github.kr328.clash.service.util.intent
import com.github.kr328.clash.service.util.startForegroundServiceCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    companion object {
        private const val REQUEST_CODE = 40000
    }

    private var bandwidthJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        status.setOnClickListener {
            if (clashRunning) {
                stopService(ClashService::class.intent)
            } else {
                val vpnRequest = VpnService.prepare(this)
                if (vpnRequest == null)
                    startForegroundServiceCompat(ClashService::class.intent)
                else
                    startActivityForResult(vpnRequest, REQUEST_CODE)
            }
        }

        proxies.setOnClickListener {
            startActivity(ProxiesActivity::class.intent)
        }

        profiles.setOnClickListener {
            startActivity(ProfilesActivity::class.intent)
        }

        logs.setOnClickListener {
            startActivity(LogsActivity::class.intent)
        }
    }

    override fun onStart() {
        super.onStart()

        updateClashStatus()
    }

    override fun onStop() {
        super.onStop()

        stopBandwidthPolling()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK)
                startForegroundServiceCompat(ClashService::class.intent)
            return
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override suspend fun onClashStarted() {
        updateClashStatus()
    }

    override suspend fun onClashStopped(reason: String?) {
        updateClashStatus()

        if (reason != null)
            makeSnackbarException(getString(R.string.clash_start_failure), reason)
    }

    private fun startBandwidthPolling() {
        if (bandwidthJob != null)
            return

        bandwidthJob = launch {
            withClash {
                try {
                    while (clashRunning && isActive) {
                        val bandwidth = queryBandwidth()
                        status.summary = getString(
                            R.string.format_traffic_forwarded,
                            bandwidth.asBytesString()
                        )
                        delay(1000)
                    }
                } finally {
                    bandwidthJob = null
                }
            }
        }
    }

    private fun stopBandwidthPolling() {
        bandwidthJob?.cancel()
    }

    private fun updateClashStatus() {
        if (clashRunning) {
            startBandwidthPolling()

            status.setCardBackgroundColor(getColor(R.color.primaryCardColorStarted))
            status.icon = getDrawable(R.drawable.ic_started)
            status.title = getText(R.string.running)

            proxies.visibility = View.VISIBLE
        } else {
            stopBandwidthPolling()

            status.setCardBackgroundColor(getColor(R.color.primaryCardColorStopped))
            status.icon = getDrawable(R.drawable.ic_stopped)
            status.title = getText(R.string.stopped)
            status.summary = getText(R.string.tap_to_start)

            proxies.visibility = View.GONE
        }
    }
}