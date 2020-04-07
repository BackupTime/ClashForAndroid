package com.github.kr328.clash.common.ids

import com.github.kr328.clash.common.BuildConfig

object Intents {
    const val INTENT_ACTION_CLASH_STARTED =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action.clash.started"
    const val INTENT_ACTION_CLASH_STOPPED =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action.clash.stopped"
    const val INTENT_ACTION_PROFILE_CHANGED =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action.profile.changed"
    const val INTENT_ACTION_PROFILE_ENQUEUE_REQUEST =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action.profile.enqueue.request"
    const val INTENT_ACTION_PROFILE_SETUP =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action.profile.setup"
    const val INTENT_ACTION_PROFILE_LOADED =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action.profile.loaded"
    const val INTENT_ACTION_NETWORK_CHANGED =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action.network.changed"
    const val INTENT_ACTION_REQUEST_STOP =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action.request.stop"

    const val INTENT_EXTRA_CLASH_STOP_REASON =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.extra.clash.stop.reason"
    const val INTENT_EXTRA_PROFILE_REQUEST =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.extra.profile.request"
    const val INTENT_EXTRA_START_TUN =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.extra.start.tun"
    const val INTENT_EXTRA_PROFILE_ID =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.extra.profile.id"
}