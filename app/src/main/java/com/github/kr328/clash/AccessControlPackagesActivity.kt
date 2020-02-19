package com.github.kr328.clash

import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.kr328.clash.adapter.PackagesAdapter
import kotlinx.android.synthetic.main.activity_access_control_packages.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.streams.toList

class AccessControlPackagesActivity: BaseActivity() {
    override val activityLabel: CharSequence?
        get() = getText(R.string.access_control_packages)
    private val activity: AccessControlPackagesActivity
        get() = this
    private val refreshChannel = Channel<Unit>(Channel.CONFLATED)

    private var keyword: String = ""
    private var sort: PackagesAdapter.Sort = PackagesAdapter.Sort.NAME
    private var decrease: Boolean = false
    private var systemApp: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_access_control_packages)
        setSupportActionBar(toolbar)

        launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = packageManager
                val packages = pm.getInstalledPackages(0)

                packages.parallelStream()
                    .map {
                        PackagesAdapter.AppInfo(
                            it.packageName,
                            it.applicationInfo.loadLabel(pm).toString(),
                            it.applicationInfo.loadIcon(pm),
                            it.firstInstallTime, it.lastUpdateTime,
                            it.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0)
                    }
                    .sorted(compareBy(PackagesAdapter.AppInfo::label))
                    .toList()
            }

            val adapter = PackagesAdapter(activity, apps)

            mainList.adapter = adapter
            mainList.layoutManager = LinearLayoutManager(activity)

            progress.visibility = View.GONE

            refreshChannel.send(Unit)

            while ( isActive ) {
                refreshChannel.receive()

                adapter.applyFilter(keyword, sort, decrease, systemApp)

                delay(200)
            }
        }
    }
}