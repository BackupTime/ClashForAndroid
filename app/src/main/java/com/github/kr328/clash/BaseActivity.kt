package com.github.kr328.clash

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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

    val isClashRunning: Boolean
        get() {
            return EmptyBroadcastReceiver().peekService(
                this,
                Intent(this, ClashService::class.java)
            ) != null
        }

    open suspend fun onClashStarted() {}
    open suspend fun onClashStopped(reason: String?) {}
    open suspend fun onClashProfileChanged(active: ClashProfileEntity?) {}

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
}