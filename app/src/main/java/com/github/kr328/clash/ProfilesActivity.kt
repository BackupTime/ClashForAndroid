package com.github.kr328.clash

import android.os.Bundle
import android.util.TypedValue
import android.view.ViewGroup.LayoutParams
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.adapter.ProfileAdapter
import com.github.kr328.clash.design.view.SettingsLayout
import com.github.kr328.clash.remote.withProfile
import com.github.kr328.clash.service.ProfileBackgroundService
import com.github.kr328.clash.service.data.ClashProfileEntity
import com.github.kr328.clash.service.util.intent
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.activity_profiles.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ProfilesActivity : BaseActivity(), ProfileAdapter.Callback {
    private var backgroundJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profiles)
        setSupportActionBar(toolbar)

        mainList.layoutManager = LinearLayoutManager(this)
        mainList.adapter = ProfileAdapter(this, this)
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
        val profiles = withProfile {
            queryProfiles()
        }

        (mainList.adapter as ProfileAdapter)
            .setEntitiesAsync(profiles.toList())
    }

    override fun onProfileClicked(entity: ClashProfileEntity) {
        launch {
            withProfile {
                setActiveProfile(entity.id)
            }
        }
    }

    override fun onMenuClicked(entity: ClashProfileEntity) {
        val dialog = BottomSheetDialog(this)
        val menu = SettingsLayout(this).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }
        @ColorInt
        val errorColor = TypedValue().run {
            theme.resolveAttribute(R.attr.colorError, this, true)
            data
        }

        menu.build {
            if (entity.type != ClashProfileEntity.TYPE_FILE) {
                option(
                    title = getString(R.string.update),
                    icon = getDrawable(R.drawable.ic_update)
                ) {

                }
            } else {
                option(
                    title = getString(R.string.edit),
                    icon = getDrawable(R.drawable.ic_edit)
                ) {

                }
            }
            option(
                title = getString(R.string.properties),
                icon = getDrawable(R.drawable.ic_properties)
            ) {

            }
            option(
                title = getString(R.string.clear_cache),
                icon = getDrawable(R.drawable.ic_delete_sweep)
            ) {

            }
            option(
                title = getString(R.string.delete),
                icon = getDrawable(R.drawable.ic_delete_colorful)
            ) {
                textColor = errorColor
            }
        }

        dialog.setContentView(menu)
        dialog.show()
    }

    override fun onNewProfile() {
        startActivity(CreateProfileActivity::class.intent)
    }

    private fun sendDelete(entity: ClashProfileEntity) {
        launch {

        }
    }
}