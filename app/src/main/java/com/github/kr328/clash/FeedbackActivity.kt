package com.github.kr328.clash

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.Keep
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.android.synthetic.main.activity_feedback.*

class FeedbackActivity : BaseActivity() {
    companion object {
        const val KEY_FEEDBACK_ID = "feedback_id"
    }

    @Keep
    class Fragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.feedback, rootKey)

            findPreference<Preference>(KEY_FEEDBACK_ID)?.apply {
                summary = MainApplication.userIdentifier

                onPreferenceClickListener = Preference.OnPreferenceClickListener { p ->
                    val data = ClipData.newPlainText("userIdentifier", summary)

                    requireContext().getSystemService(ClipboardManager::class.java)
                        ?.setPrimaryClip(data)

                    Toast.makeText(
                        requireContext(),
                        R.string.feedback_feedback_id_copied,
                        Toast.LENGTH_LONG
                    ).show()

                    true
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        setSupportActionBar(activity_feedback_toolbar)
    }
}