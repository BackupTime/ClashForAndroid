package com.github.kr328.clash

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.adapter.ProfileAdapter
import com.github.kr328.clash.common.ids.Intents
import com.github.kr328.clash.common.util.componentName
import com.github.kr328.clash.common.util.intent
import com.github.kr328.clash.common.util.startForegroundServiceCompat
import com.github.kr328.clash.common.utils.Log
import com.github.kr328.clash.remote.withProfile
import com.github.kr328.clash.service.ProfileBackgroundService
import com.github.kr328.clash.service.ProfileProvider
import com.github.kr328.clash.service.model.ProfileMetadata
import com.github.kr328.clash.weight.ProfilesMenu
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_profiles.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.io.FileNotFoundException
import java.util.*

class ProfilesActivity : BaseActivity(), ProfileAdapter.Callback, ProfilesMenu.Callback {
    companion object {
        private const val EDITOR_REQUEST_CODE = 30000
    }

    private val self = this
    private var backgroundJob: Job? = null
    private val reloadMutex = Mutex()
    private val editorStack = Stack<Long>()

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == EDITOR_REQUEST_CODE) {
            launch {
                val id = editorStack.pop()

                withProfile {
                    if (resultCode == Activity.RESULT_OK)
                        commitAsync(id)
                    else
                        ProfileProvider.releaseTemp(self, id)
                }
            }

            return
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override suspend fun onClashProfileChanged() {
        reloadProfiles()
    }

    private suspend fun reloadProfiles() {
        if (!reloadMutex.tryLock())
            return

        val profiles = withProfile {
            queryAll()
        }

        (mainList.adapter as ProfileAdapter).setEntitiesAsync(profiles.toList())

        reloadMutex.unlock()
    }

    override fun onProfileClicked(entity: ProfileMetadata) {
        launch {
            withProfile {
                setActive(entity.id)
            }
        }
    }

    override fun onMenuClicked(entity: ProfileMetadata) {
        ProfilesMenu(this, entity, this).show()
    }

    override fun onNewProfile() {
        startActivity(CreateProfileActivity::class.intent)
    }

    private fun openProperties(id: Long) {
        if (id < 0) {
            Snackbar.make(rootView, getText(R.string.profile_not_found), Snackbar.LENGTH_LONG)
                .show()
            return
        }

        startActivity(ProfileEditActivity::class.intent.putExtra("id", id))
    }

    private fun openEditor(id: Long) = launch {
        try {
            if (id < 0)
                throw FileNotFoundException()

            val uri = ProfileProvider.acquireTemp(self, id)

            editorStack.push(id)

            startActivityForResult(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "text/plain")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
                EDITOR_REQUEST_CODE
            )
        } catch (e: Exception) {
            Snackbar.make(rootView, getText(R.string.profile_not_found), Snackbar.LENGTH_LONG)
                .show()
        }
    }

    private fun startUpdate(id: Long) {
        val intent = Intent(Intents.INTENT_ACTION_PROFILE_REQUEST_UPDATE)
            .setComponent(ProfileBackgroundService::class.componentName)
            .putExtra(Intents.INTENT_EXTRA_PROFILE_ID, id)

        startForegroundServiceCompat(intent)
    }

    override fun onOpenEditor(entity: ProfileMetadata) {
        openEditor(entity.id)
    }

    override fun onUpdate(entity: ProfileMetadata) {
        startUpdate(entity.id)
    }

    override fun onOpenProperties(entity: ProfileMetadata) {
        openProperties(entity.id)
    }

    override fun onDuplicate(entity: ProfileMetadata) {
        launch {
            withProfile {
                openProperties(acquireCloned(entity.id))
            }
        }
    }

    override fun onResetProvider(entity: ProfileMetadata) {
        launch {
            withProfile {
                clear(entity.id)
            }
        }
    }

    override fun onDelete(entity: ProfileMetadata) {
        launch {
            withProfile {
                delete(entity.id)
            }
        }
    }
}