package com.github.kr328.clash

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.github.kr328.clash.remote.Broadcasts
import com.github.kr328.clash.service.data.ClashProfileEntity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

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

        override fun onProfileChanged(active: ClashProfileEntity?) {
            launch {
                onClashProfileChanged(active)
            }
        }
    }

    private var overrideRootView: View? = null

    val clashRunning: Boolean
        get() = Broadcasts.clashRunning
    val rootView: View
        get() = overrideRootView ?: window.decorView

    open suspend fun onClashStarted() {}
    open suspend fun onClashStopped(reason: String?) {}
    open suspend fun onClashProfileChanged(active: ClashProfileEntity?) {}

    override fun setContentView(layoutResID: Int) {
        val base = FrameLayout(this).apply {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        resetLightNavigationBar()
    }

    override fun onStart() {
        super.onStart()

        Broadcasts.register(receiver)
    }

    override fun onStop() {
        super.onStop()

        Broadcasts.unregister(receiver)
    }

    override fun setSupportActionBar(toolbar: Toolbar?) {
        super.setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(shouldDisplayHomeAsUpEnabled())
    }

    open fun shouldDisplayHomeAsUpEnabled(): Boolean {
        return true
    }

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

    protected fun makeSnackbarException(title: String, detail: String) {
        Snackbar.make(rootView, title, Snackbar.LENGTH_LONG).setAction(R.string.detail) {
            AlertDialog.Builder(this).setTitle(R.string.detail).setMessage(detail).show()
        }.show()
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