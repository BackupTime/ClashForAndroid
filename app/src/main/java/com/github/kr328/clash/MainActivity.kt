package com.github.kr328.clash

import android.content.Intent
import android.os.Bundle
import com.github.kr328.clash.core.utils.asBytesString
import com.github.kr328.clash.remote.withClash
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    private var bandwidthJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        profiles.setOnClickListener {
            startActivity(Intent(this, ProfilesActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()

        launch {
            if (clashRunning) {
                status.icon = getDrawable(R.drawable.ic_started)
                status.title = getText(R.string.clash_status_started)
                status.summary = getString(
                    R.string.clash_status_forwarded_traffic,
                    0L.asBytesString()
                )
            }

            startBandwidthPolling()
        }
    }

    override fun onStop() {
        super.onStop()

        stopBandwidthPolling()
    }

    override suspend fun onClashStarted() {
        super.onClashStarted()

        startBandwidthPolling()
    }

    override suspend fun onClashStopped(reason: String?) {
        super.onClashStopped(reason)

        stopBandwidthPolling()
    }

    private fun startBandwidthPolling() {
        if (bandwidthJob != null)
            return

        bandwidthJob = launch {
            withClash {
                while (clashRunning && isActive) {
                    val bandwidth = queryBandwidth()
                    status.summary = getString(
                        R.string.clash_status_forwarded_traffic,
                        bandwidth.asBytesString()
                    )
                    delay(1000)
                }
                bandwidthJob = null
            }
        }
    }

    private fun stopBandwidthPolling() {
        bandwidthJob?.cancel()
    }
}