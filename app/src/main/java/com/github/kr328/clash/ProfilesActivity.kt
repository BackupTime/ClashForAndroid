package com.github.kr328.clash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.adapter.ProfileAdapter
import com.github.kr328.clash.remote.withProfile
import com.github.kr328.clash.component.ids.Intents
import com.github.kr328.clash.service.ProfileBackgroundService
import com.github.kr328.clash.service.data.ClashProfileEntity
import com.github.kr328.clash.service.transact.ProfileRequest
import com.github.kr328.clash.service.util.componentName
import com.github.kr328.clash.service.util.intent
import com.github.kr328.clash.service.util.startForegroundServiceCompat
import com.github.kr328.clash.weight.ProfilesMenu
import kotlinx.android.synthetic.main.activity_profiles.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.util.*

class ProfilesActivity : BaseActivity(), ProfileAdapter.Callback, ProfilesMenu.Callback {
    companion object {
        private const val EDITOR_REQUEST_CODE = 30000
    }

    private var backgroundJob: Job? = null
    private val reloadMutex = Mutex()
    private val editorStack = Stack<String>()

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
                val uri = editorStack.pop()

                withProfile {
                    commitProfileEditUri(uri)
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
            queryProfiles()
        }

        (mainList.adapter as ProfileAdapter)
            .setEntitiesAsync(profiles.toList())

        reloadMutex.unlock()
    }

    override fun onProfileClicked(entity: ClashProfileEntity) {
        launch {
            withProfile {
                setActiveProfile(entity.id)
            }
        }
    }

    override fun onMenuClicked(entity: ClashProfileEntity) {
        ProfilesMenu(this, entity, this).show()
    }

    override fun onNewProfile() {
        startActivity(CreateProfileActivity::class.intent)
    }

    private fun deleteProfile(entity: ClashProfileEntity) = launch {
        val request = ProfileRequest().action(ProfileRequest.Action.REMOVE).withId(entity.id)

        withProfile {
            enqueueRequest(request)
        }
    }

    private fun resetProviders(entity: ClashProfileEntity) = launch {
        val request = ProfileRequest().action(ProfileRequest.Action.CLEAR).withId(entity.id)

        withProfile {
            enqueueRequest(request)
        }
    }

    private fun openPropertiesEditor(entity: ClashProfileEntity, duplicate: Boolean) {
        val type = when (entity.type) {
            ClashProfileEntity.TYPE_FILE ->
                Constants.URL_PROVIDER_TYPE_FILE
            ClashProfileEntity.TYPE_URL ->
                Constants.URL_PROVIDER_TYPE_URL
            ClashProfileEntity.TYPE_EXTERNAL ->
                Constants.URL_PROVIDER_TYPE_EXTERNAL
            else -> throw IllegalArgumentException("Invalid type ${entity.type}")
        }
        val intent = entity.source?.run { Intent.parseUri(this, 0) }
        val name = entity.name
        val uri = entity.uri
        val interval = entity.updateInterval.toString()

        val editor = ProfileEditActivity::class.intent
            .putExtra("id", if (duplicate) -1L else entity.id)
            .putExtra("type", if (duplicate) Constants.URL_PROVIDER_TYPE_FILE else type)
            .putExtra("intent", intent)
            .putExtra("name", name)
            .putExtra("url", uri)
            .putExtra("interval", if (duplicate) "0" else interval)

        startActivity(editor)
    }

    private fun openEditor(entity: ClashProfileEntity) = launch {
        val uri = withProfile {
            requestProfileEditUri(entity.id)
        } ?: return@launch

        editorStack.push(uri)

        startActivityForResult(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(Uri.parse(uri), "text/plain")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
            EDITOR_REQUEST_CODE
        )
    }

    private fun startUpdate(entity: ClashProfileEntity) {
        val request = ProfileRequest()
            .action(ProfileRequest.Action.UPDATE_OR_CREATE)
            .withId(entity.id)

        val intent = Intent(Intents.INTENT_ACTION_PROFILE_ENQUEUE_REQUEST)
            .setComponent(ProfileBackgroundService::class.componentName)
            .putExtra(Intents.INTENT_EXTRA_PROFILE_REQUEST, request)

        startForegroundServiceCompat(intent)
    }

    override fun onOpenEditor(entity: ClashProfileEntity) {
        openEditor(entity)
    }

    override fun onUpdate(entity: ClashProfileEntity) {
        startUpdate(entity)
    }

    override fun onOpenProperties(entity: ClashProfileEntity) {
        openPropertiesEditor(entity, false)
    }

    override fun onDuplicate(entity: ClashProfileEntity) {
        openPropertiesEditor(entity, true)
    }

    override fun onResetProvider(entity: ClashProfileEntity) {
        resetProviders(entity)
    }

    override fun onDelete(entity: ClashProfileEntity) {
        deleteProfile(entity)
    }
}