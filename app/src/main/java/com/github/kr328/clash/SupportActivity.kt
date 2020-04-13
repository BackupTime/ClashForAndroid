package com.github.kr328.clash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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