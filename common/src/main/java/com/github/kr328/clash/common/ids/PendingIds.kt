package com.github.kr328.clash.common.ids

object PendingIds {
    fun generateProfileResultId(profileId: Long): Int {
        return NotificationIds.generateProfileResultId(profileId)
    }
}