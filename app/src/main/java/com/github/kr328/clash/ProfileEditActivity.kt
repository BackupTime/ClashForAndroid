package com.github.kr328.clash

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.View
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AlertDialog
import com.github.kr328.clash.core.utils.Log
import com.github.kr328.clash.design.common.TextInput
import com.github.kr328.clash.remote.withProfile
import com.github.kr328.clash.service.data.ClashProfileEntity
import com.github.kr328.clash.service.ipc.IStreamCallback
import com.github.kr328.clash.service.ipc.ParcelableContainer
import com.github.kr328.clash.service.transact.ProfileRequest
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_profile_edit.*
import kotlinx.coroutines.launch

class ProfileEditActivity : BaseActivity() {
    companion object {
        private const val REQUEST_CODE = 10000

        private const val KEY_NAME = "name"
        private const val KEY_URL = "url"
        private const val KEY_AUTO_UPDATE = "auto_update"

        private val TYPE_YAML = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension("yaml") ?: "*/*"
    }

    private var modified = false
    private var processing = false
        set(value) {
            field = value

            if (value) {
                saving.visibility = View.VISIBLE
                save.visibility = View.INVISIBLE
            } else {
                saving.visibility = View.INVISIBLE
                save.visibility = View.VISIBLE
            }
        }

    private val requestCallback = object : IStreamCallback.Stub() {
        override fun complete() {
            launch {
                setResult(Activity.RESULT_OK)
                finish()
            }
        }

        override fun completeExceptionally(reason: String?) {
            launch {
                makeSnackbarException(getString(R.string.download_failure), reason ?: "Unknown")
                processing = false
            }
        }

        override fun send(data: ParcelableContainer?) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)
        setSupportActionBar(toolbar)

        settings.build {
            tips(icon = getDrawable(R.drawable.ic_info)) {
                title = Html.fromHtml(getString(R.string.tips_profile), Html.FROM_HTML_MODE_LEGACY)
            }

            textInput(
                title = getString(R.string.name),
                icon = getDrawable(R.drawable.ic_label_outline),
                hint = getString(R.string.profile_name),
                content = intent.getStringExtra("name") ?: "",
                id = KEY_NAME
            ) {
                onTextChanged {
                    modified = true
                }
            }
            textInput(
                title = getString(R.string.url),
                icon = getDrawable(R.drawable.ic_content),
                hint = getString(R.string.profile_url),
                content = intent.getStringExtra("url") ?: "",
                id = KEY_URL
            ) {
                onOpenInput {
                    if (!openUrlProvider())
                        openDialogInput()
                }
                onDisplayContent {
                    it.split("/").last()
                }
                onTextChanged {
                    modified = true
                }
            }
            textInput(
                title = getString(R.string.auto_update),
                icon = getDrawable(R.drawable.ic_update),
                hint = getString(R.string.seconds),
                id = KEY_AUTO_UPDATE,
                content = intent.getStringExtra("interval") ?: ""
            ) {
                onDisplayContent {
                    val interval = it.toString().toIntOrNull() ?: 0

                    if (interval <= 0)
                        getString(R.string.disabled)
                    else
                        getString(R.string.format_seconds, interval)
                }
                onTextChanged {
                    val s = it.toString()

                    if (s.isNotEmpty() && s.toIntOrNull() == null) {
                        content = ""
                        Snackbar.make(rootView, R.string.invalid_interval, Snackbar.LENGTH_LONG)
                            .show()
                    } else {
                        modified = true
                    }
                }

                if (intent.getStringExtra("type") == Constants.URL_PROVIDER_TYPE_FILE)
                    isHidden = true
            }
        }

        settings.screen.restoreState(savedInstanceState)

        save.setOnClickListener {
            with(settings.screen) {
                val name = requireElement<TextInput>(KEY_NAME).content.toString()
                val url = Uri.parse(requireElement<TextInput>(KEY_URL).content.toString())
                val interval = requireElement<TextInput>(KEY_AUTO_UPDATE).content.toString()
                    .toLongOrNull() ?: 0

                if (name.isBlank()) {
                    Snackbar.make(rootView, R.string.empty_name, Snackbar.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                if (url == null || url == Uri.EMPTY ||
                    (!url.scheme.equals("http", ignoreCase = true)
                            && !url.scheme.equals("https", ignoreCase = true)
                            && !url.scheme.equals("content", ignoreCase = true)
                            && !url.scheme.equals("file", ignoreCase = true))
                ) {
                    Snackbar.make(rootView, R.string.invalid_url, Snackbar.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                processing = true

                sendProfileRequest(name, url, interval)
            }
        }

        when (intent.extras?.getLong("id", Long.MIN_VALUE)) {
            Long.MIN_VALUE -> {
                openUrlProvider()
                setTitle(R.string.new_profile)
            }
            -1L -> {
                setTitle(R.string.new_profile)
            }
            else -> {
                setTitle(R.string.edit_profile)
            }
        }
    }

    override val activityLabel: CharSequence? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK || data == null)
                return

            data.data?.apply {
                settings.screen.requireElement<TextInput>(KEY_URL).content = this.toString()
            }

            data.getStringExtra(Constants.URL_PROVIDER_INTENT_EXTRA_NAME)?.also {
                settings.screen.requireElement<TextInput>(KEY_NAME).apply {
                    if (content.isBlank())
                        content = it
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onBackPressed() {
        if (!modified)
            return super.onBackPressed()

        if (processing) {
            Snackbar.make(rootView, R.string.processing, Snackbar.LENGTH_LONG).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.exit_without_save)
            .setMessage(R.string.exit_without_save_warning)
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .setPositiveButton(R.string.ok) { _, _ -> finish() }
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        settings.screen.saveState(outState)
    }

    private fun openUrlProvider(): Boolean {
        val type = intent.getStringExtra("type")
        val externalIntent = intent.getParcelableExtra<Intent>("intent")

        try {
            when (type) {
                Constants.URL_PROVIDER_TYPE_FILE ->
                    startActivityForResult(
                        Intent(Intent.ACTION_GET_CONTENT).setType(TYPE_YAML),
                        REQUEST_CODE
                    )
                Constants.URL_PROVIDER_TYPE_EXTERNAL ->
                    startActivityForResult(
                        externalIntent ?: throw NullPointerException(),
                        REQUEST_CODE
                    )
                else -> return false
            }
        } catch (e: Exception) {
            makeSnackbarException(getString(R.string.start_url_provider_failure), e.message)
        }

        return true
    }

    private fun sendProfileRequest(name: String, url: Uri, interval: Long) {
        launch {
            val source = intent?.getParcelableExtra<Intent>("intent")?.toUri(0)?.run(Uri::parse)
            val type = when (intent?.getStringExtra("type")) {
                Constants.URL_PROVIDER_TYPE_FILE ->
                    ClashProfileEntity.TYPE_FILE
                Constants.URL_PROVIDER_TYPE_URL ->
                    ClashProfileEntity.TYPE_URL
                Constants.URL_PROVIDER_TYPE_EXTERNAL ->
                    ClashProfileEntity.TYPE_EXTERNAL
                else -> throw IllegalArgumentException()
            }

            Log.d(interval.toString())

            val request = ProfileRequest()
                .action(ProfileRequest.Action.UPDATE_OR_CREATE)
                .withId(intent.getLongExtra("id", -1L))
                .withName(name)
                .withURL(url)
                .withUpdateInterval(interval)
                .withCallback(requestCallback)
                .withType(type)
                .withSource(source)

            withProfile {
                enqueueRequest(request)
            }
        }
    }
}