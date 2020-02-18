package com.github.kr328.clash.service

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

    const val INTENT_EXTRA_CLASH_STOP_REASON =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.extra.clash.stop.reason"
    const val INTENT_EXTRA_PROFILE =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.extra.profile"
    const val INTENT_EXTRA_PROFILE_REQUEST =
        "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.extra.profile.request"
}