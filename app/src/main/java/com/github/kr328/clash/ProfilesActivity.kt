package com.github.kr328.clash

import android.os.Bundle
import com.github.kr328.clash.remote.withClash
import com.github.kr328.clash.service.data.ClashProfileEntity
import kotlinx.android.synthetic.main.activity_profiles.*
import kotlinx.coroutines.launch

class ProfilesActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_profiles)

        setSupportActionBar(toolbar)

        reloadProfiles()
    }

    override suspend fun onClashProfileChanged(active: ClashProfileEntity?) {
        super.onClashProfileChanged(active)

        reloadProfiles()
    }

    private fun reloadProfiles() {
        launch {
            withClash {

            }
        }
    }
}