package com.github.kr328.clash

import android.os.Bundle
import com.github.kr328.clash.preference.UiSettings
import com.github.kr328.clash.service.settings.ServiceSettings
import com.github.kr328.clash.settings.NetworkFragment
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsNetworkActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fragment)
        setSupportActionBar(findViewById(R.id.toolbar))

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment, NetworkFragment())
            .commit()
    }
}