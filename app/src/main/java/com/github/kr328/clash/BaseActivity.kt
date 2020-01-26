package com.github.kr328.clash

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.github.kr328.clash.remote.Broadcasts
import com.github.kr328.clash.service.ClashService
import com.github.kr328.clash.service.data.ClashProfileEntity
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

    var clashRunning: Boolean = false
        private set

    open suspend fun onClashStarted() {
        clashRunning = true
    }

    open suspend fun onClashStopped(reason: String?) {
        clashRunning = false
    }

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

        LayoutInflater.from(this).inflate(layoutResID, base, true)

        super.setContentView(base)
    }

    override fun onStart() {
        super.onStart()

        Broadcasts.register(receiver)

        clashRunning = EmptyBroadcastReceiver().peekService(
            this,
            Intent(this, ClashService::class.java)
        ) != null
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
}