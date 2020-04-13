package com.github.kr328.clash

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Html
import kotlinx.android.synthetic.main.activity_support.*

class SupportActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_support)
        setSupportActionBar(toolbar)

        commonUi.build {
            tips {
                icon = getDrawable(R.drawable.ic_info)
                title = Html.fromHtml(getString(R.string.tips_support), Html.FROM_HTML_MODE_LEGACY)
            }

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

            category(text = getString(R.string.feedback))

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

            val firstLanguage = resources.configuration.locales.get(0).language

            if (firstLanguage.equals("zh", true)) {
                category(getString(R.string.donate))

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