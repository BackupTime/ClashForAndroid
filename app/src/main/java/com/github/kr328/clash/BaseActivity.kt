package com.github.kr328.clash

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.github.kr328.clash.preference.UiSettings
import com.github.kr328.clash.remote.Broadcasts
import com.github.kr328.clash.service.data.ClashProfileEntity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.*

abstract class BaseActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    class EmptyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {}
    }

    private val receiver = object : Broadcasts.Receiver {
        override fun onStarted() {
            launch {
                onClashStarted()
            }
        }

        override fun onStopped(cause: String?) {
            launch {
                onClashStopped(cause)
            }
        }

        override fun onProfileChanged() {
            launch {
                onClashProfileChanged()
            }
        }

        override fun onProfileLoaded(profileEntity: ClashProfileEntity) {
            launch {
                onClashProfileLoaded(profileEntity)
            }
        }
    }

    private var overrideRootView: View? = null

    val clashRunning: Boolean
        get() = Broadcasts.clashRunning
    val rootView: View
        get() = overrideRootView ?: window.decorView
    var menu: Menu? = null
    lateinit var uiSettings: UiSettings
        private set
    lateinit var language: String
    lateinit var darkMode: String

    open suspend fun onClashStarted() {}
    open suspend fun onClashStopped(reason: String?) {}
    open suspend fun onClashProfileChanged() {}
    open suspend fun onClashProfileLoaded(profile: ClashProfileEntity) {}

    override fun setContentView(layoutResID: Int) {
        val base = CoordinatorLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            val displayMetrics = resources.displayMetrics

            if (displayMetrics.widthPixels > displayMetrics.heightPixels) {
                val padding = (displayMetrics.widthPixels - displayMetrics.heightPixels) / 2

                setPadding(padding, 0, padding, 0)
            }
        }

        overrideRootView = LayoutInflater.from(this).inflate(layoutResID, base, true)

        super.setContentView(base)
    }

    override fun attachBaseContext(newBase: Context?) {
        val base = newBase ?: return super.attachBaseContext(newBase)

        uiSettings = UiSettings(base)

        language = uiSettings.get(UiSettings.LANGUAGE)

        val languageOverride = language.split("-")
        if (language.isEmpty())
            return super.attachBaseContext(base)

        val configuration = base.resources.configuration
        val localeOverride = if (languageOverride.size == 2)
            Locale(languageOverride[0], languageOverride[1])
        else
            Locale(languageOverride[0])

        configuration.setLocale(localeOverride)

        super.attachBaseContext(base.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        resetDarkMode()

        resetLightNavigationBar()
    }

    override fun onStart() {
        super.onStart()

        if (language != uiSettings.get(UiSettings.LANGUAGE))
            recreate()
        if (darkMode != uiSettings.get(UiSettings.DARK_MODE)) {
            resetDarkMode()
            recreate()
        }

        Broadcasts.register(receiver)
    }

    override fun onStop() {
        super.onStop()

        Broadcasts.unregister(receiver)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (super.onOptionsItemSelected(item))
            return true

        if (item.itemId == android.R.id.home) {
            onSupportNavigateUp()
            return true
        }

        return false
    }

    override fun setSupportActionBar(toolbar: Toolbar?) {
        super.setSupportActionBar(toolbar)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(shouldDisplayHomeAsUpEnabled())

            activityLabel?.let {
                title = it
            }
        }
    }

    open fun shouldDisplayHomeAsUpEnabled(): Boolean {
        return true
    }

    abstract val activityLabel: CharSequence?

    override fun onSupportNavigateUp(): Boolean {
        this.onBackPressed()

        return true
    }

    override fun onDestroy() {
        cancel()

        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        recreate()
    }

    protected fun makeSnackbarException(title: String, detail: String?) {
        Snackbar.make(rootView, title, Snackbar.LENGTH_LONG).setAction(R.string.detail) {
            AlertDialog.Builder(this).setTitle(R.string.detail).setMessage(detail ?: "Unknown")
                .show()
        }.show()
    }

    private fun resetDarkMode() {
        when ( uiSettings.get(UiSettings.DARK_MODE).also { darkMode = it } ) {
            UiSettings.DARK_MODE_AUTO ->
                delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            UiSettings.DARK_MODE_DARK ->
                delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
            UiSettings.DARK_MODE_LIGHT ->
                delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_NO
        }
    }

    private fun resetLightNavigationBar() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return

        val light = resources.getBoolean(R.bool.lightStatusBar)

        if (light) {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility or SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        } else {
            window.decorView.systemUiVisibility =
                window.decorView.systemUiVisibility and SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        }

        window.navigationBarColor = getColor(R.color.backgroundColor)
    }
}