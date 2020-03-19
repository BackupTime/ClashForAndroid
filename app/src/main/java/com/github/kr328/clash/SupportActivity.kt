package com.github.kr328.clash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.content.getSystemService
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_support.*

class SupportActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_support)
        setSupportActionBar(toolbar)

        commonUi.build {
            category(text = getString(R.string.sources))

            option(
                title = getString(R.string.clash),
                summary = getString(R.string.clash_url)
            ) {
                onClick {
                    startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(getString(R.string.clash_url)))
                    )
                }
            }
            option(
                title = getString(R.string.clash_for_android),
                summary = getString(R.string.clash_for_android_url)
            ) {
                onClick {
                    startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(getString(R.string.clash_for_android_url)))
                    )
                }
            }

            category(text = getString(R.string.contacts))

            option(
                title = getString(R.string.email),
                summary = getString(R.string.email_url)
            ) {
                onClick {
                    val data =
                        ClipData.newPlainText("email", getText(R.string.email_url))
                    getSystemService<ClipboardManager>()?.setPrimaryClip(data)

                    Snackbar.make(rootView, getText(R.string.copied), Snackbar.LENGTH_SHORT).show()
                }
            }
            option(
                title = getString(R.string.github_issues),
                summary = getString(R.string.github_issues_url)
            ) {
                onClick {
                    startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(getString(R.string.github_issues_url)))
                    )
                }
            }

            if (resources.configuration.locales.get(0)
                    .language.equals("zh", true)
            ) {
                option(
                    title = getString(R.string.telegram_channel),
                    summary = getString(R.string.telegram_channel_url)
                ) {
                    onClick {
                        startActivity(
                            Intent(Intent.ACTION_VIEW)
                                .setData(Uri.parse(getString(R.string.telegram_channel_url)))
                        )
                    }
                }
            }
        }
    }
}