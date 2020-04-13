package com.github.kr328.clash.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.fragment.app.Fragment
import com.github.kr328.clash.Constants
import com.github.kr328.clash.R
import com.github.kr328.clash.design.common.TextInput
import com.github.kr328.clash.design.view.CommonUiLayout
import com.github.kr328.clash.service.model.ProfileMetadata.Type
import com.google.android.material.snackbar.Snackbar

class ProfileEditFragment(
    var name: String,
    var uri: Uri,
    var interval: Long,
    private val type: Type,
    private val source: Uri?
) : Fragment() {
    companion object {
        private const val REQUEST_CODE = 10000

        private const val KEY_NAME = "name"
        private const val KEY_URL = "url"
        private const val KEY_AUTO_UPDATE = "auto_update"

        private val TYPE_YAML = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension("yaml") ?: "*/*"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return CommonUiLayout(requireContext()).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

            build {
                tips(icon = requireContext().getDrawable(R.drawable.ic_info)) {
                    title =
                        Html.fromHtml(getString(R.string.tips_profile), Html.FROM_HTML_MODE_LEGACY)
                }

                textInput(
                    title = getString(R.string.name),
                    icon = requireContext().getDrawable(R.drawable.ic_label_outline),
                    hint = getString(R.string.profile_name),
                    id = KEY_NAME,
                    content = name
                ) {
                    onTextChanged {
                        name = content.toString()
                    }
                }
                textInput(
                    title = getString(R.string.url),
                    icon = requireContext().getDrawable(R.drawable.ic_content),
                    hint = getString(R.string.profile_url),
                    id = KEY_URL,
                    content = uri.toString()
                ) {
                    onOpenInput {
                        if (!openUrlProvider())
                            openDialogInput()
                    }
                    onDisplayContent {
                        it.split("/").last()
                    }
                    onTextChanged {
                        if (!URLUtil.isValidUrl(content.toString())) {
                            content = ""
                            Snackbar.make(view, R.string.invalid_url, Snackbar.LENGTH_LONG).show()
                            return@onTextChanged
                        }

                        uri = Uri.parse(content.toString())
                    }
                }
                textInput(
                    title = getString(R.string.auto_update),
                    icon = requireContext().getDrawable(R.drawable.ic_update),
                    hint = getString(R.string.more_than_15_minutes),
                    id = KEY_AUTO_UPDATE,
                    content = (interval / 1000 / 60).toString()
                ) {
                    onDisplayContent {
                        val interval = it.toString().toIntOrNull() ?: 0

                        if (interval <= 0)
                            getString(R.string.disabled)
                        else
                            getString(R.string.format_minutes, interval)
                    }
                    onTextChanged {
                        val s = it.toString()

                        if (s.isBlank()) {
                            content = ""
                            return@onTextChanged
                        }

                        val value = s.toIntOrNull()
                        if (value == null || value < 15) {
                            content = ""
                            Snackbar.make(view, R.string.invalid_interval, Snackbar.LENGTH_LONG)
                                .show()
                            return@onTextChanged
                        }

                        interval = content.toString().toLong() * 1000 * 60
                    }
                }

                screen.restoreState(savedInstanceState)

                if (type == Type.EXTERNAL)
                    openUrlProvider()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK || data == null)
                return

            val layout = view as CommonUiLayout

            data.data?.apply {
                layout.screen.requireElement<TextInput>(KEY_URL).content = this.toString()
            }

            data.getStringExtra(Constants.URL_PROVIDER_INTENT_EXTRA_NAME)?.also {
                layout.screen.requireElement<TextInput>(KEY_NAME).apply {
                    if (content.isBlank())
                        content = it
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun openUrlProvider(): Boolean {
        try {
            when (type) {
                Type.FILE ->
                    startActivityForResult(
                        Intent(Intent.ACTION_GET_CONTENT).setType(TYPE_YAML),
                        REQUEST_CODE
                    )
                Type.EXTERNAL ->
                    startActivityForResult(
                        Intent.parseUri(source?.toString(), 0)
                            ?: throw NullPointerException(),
                        REQUEST_CODE
                    )
                else -> return false
            }
        } catch (e: Exception) {
            Snackbar.make(
                view as ViewGroup,
                R.string.start_url_provider_failure,
                Snackbar.LENGTH_LONG
            ).show()
        }

        return true
    }
}