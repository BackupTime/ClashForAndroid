package com.github.kr328.clash.service

object Intents {
    const val INTENT_ACTION_BIND_TUN_SERVICE =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action.bind.tun"
    const val INTENT_ACTION_CLASH_STARTED =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action.clash.started"
    const val INTENT_ACTION_CLASH_STOPPED =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action.clash.stopped"
    const val INTENT_ACTION_PROFILE_CHANGED =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action.profile.changed"

    const val INTENT_EXTRA_CLASH_STOP_REASON =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.extra.clash.stop.reason"
    const val INTENT_EXTRA_PROFILE_ACTIVE =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.extra.profile.active.name"
}