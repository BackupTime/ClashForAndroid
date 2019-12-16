package com.github.kr328.clash.core.event

interface Event {
    companion object {
        const val EVENT_LOG = 1
        const val EVENT_TRAFFIC = 3
        const val EVENT_BANDWIDTH = 4
    }
}