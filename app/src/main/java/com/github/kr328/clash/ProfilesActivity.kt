package com.github.kr328.clash

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.adapter.ProfileAdapter
import com.github.kr328.clash.remote.withClash
import com.github.kr328.clash.service.data.ClashProfileEntity
import kotlinx.android.synthetic.main.activity_profiles.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ProfilesActivity : BaseActivity() {
    private var backgroundJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profiles)
        setSupportActionBar(toolbar)

        mainList.layoutManager = LinearLayoutManager(this)
        mainList.adapter = ProfileAdapter(this)
    }

    override fun onStart() {
        super.onStart()

        backgroundJob = launch {
            reloadProfiles()

            while (isActive) {
                delay(1000 * 60)

                // Refresh without animation
                (mainList.adapter as ProfileAdapter).notifyDataSetChanged()
            }
        }
    }

    override fun onStop() {
        super.onStop()

        backgroundJob?.cancel()
        backgroundJob = null
    }

    override suspend fun onClashProfileChanged(active: ClashProfileEntity?) {
        super.onClashProfileChanged(active)

        reloadProfiles()
    }

    private suspend fun reloadProfiles() {
        val profiles = withClash {
            queryProfiles()
        }

        (mainList.adapter as ProfileAdapter)
            .setEntitiesAsync(profiles.toList())
    }
}