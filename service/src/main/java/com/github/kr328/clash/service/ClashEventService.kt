package com.github.kr328.clash.service

import com.github.kr328.clash.core.event.*
import java.util.concurrent.Executors

class ClashEventService(private val master: Master) : IClashEventService.Stub() {
    interface Master {
        fun acquireEvent(event: Int)
        fun releaseEvent(event: Int)
    }

    companion object {
        private val EVENT_SET =
            setOf(Event.EVENT_LOG, Event.EVENT_TRAFFIC, Event.EVENT_BANDWIDTH)
    }

    private data class EventObserverRecord(
        val observer: IClashEventObserver,
        val acquiredEvent: Set<Int>
    )

    private val observers = mutableMapOf<String, EventObserverRecord>()
    private val executor = Executors.newSingleThreadExecutor()

    private var currentProcessEvent = ProcessEvent.STOPPED

    fun performProcessEvent(event: ProcessEvent) {
        if (!executor.isShutdown)
            executor.submit {
                currentProcessEvent = event

                observers.values.forEach {
                    it.observer.onProcessEvent(event)
                }
            }
    }

    fun performLogEvent(event: LogEvent) {
        if (!executor.isShutdown)
            executor.submit {
                observers.values.forEach {
                    if (it.acquiredEvent.contains(Event.EVENT_LOG))
                        it.observer.onLogEvent(event)
                }
            }
    }

    fun performSpeedEvent(event: TrafficEvent) {
        if (!executor.isShutdown)
            executor.submit {
                observers.values.forEach {
                    if (it.acquiredEvent.contains(Event.EVENT_TRAFFIC))
                        it.observer.onTrafficEvent(event)
                }
            }
    }

    fun performBandwidthEvent(event: BandwidthEvent) {
        if (!executor.isShutdown)
            executor.submit {
                observers.values.forEach {
                    if (it.acquiredEvent.contains(Event.EVENT_BANDWIDTH))
                        it.observer.onBandwidthEvent(event)
                }
            }
    }

    fun performErrorEvent(event: ErrorEvent) {
        if (!executor.isShutdown)
            executor.submit {
                observers.values.forEach {
                    it.observer.onErrorEvent(event)
                }
            }
    }

    fun performProfileChangedEvent(event: ProfileChangedEvent) {
        if (!executor.isShutdown)
            executor.submit {
                observers.values.forEach {
                    it.observer.onProfileChanged(event)
                }
            }
    }

    fun performProfileReloadEvent(event: ProfileReloadEvent) {
        if (!executor.isShutdown)
            executor.submit {
                observers.values.forEach {
                    it.observer.onProfileReloaded(event)
                }
            }
    }

    override fun unregisterEventObserver(id: String?) {
        if (!executor.isShutdown)
            executor.submit {
                require(id != null)

                observers.remove(id)

                recastEventRequirement()
            }
    }

    override fun registerEventObserver(
        id: String?,
        observer: IClashEventObserver?,
        events: IntArray?
    ) {
        if (!executor.isShutdown)
            executor.submit {
                require(id != null && observer != null && events != null)

                val initial = !observers.containsKey(id)

                observers[id] = EventObserverRecord(observer, events.toSet())

                observer.asBinder().linkToDeath({
                    unregisterEventObserver(id)
                }, 0)

                recastEventRequirement()

                if (initial) {
                    observer.onProcessEvent(currentProcessEvent)
                }
            }
    }

    fun recastEventRequirement() {
        if (!executor.isShutdown)
            executor.submit {
                val req = observers.values.flatMap {
                    it.acquiredEvent
                }.toSet()
                val rel = EVENT_SET - req

                req.forEach(master::acquireEvent)
                rel.forEach(master::releaseEvent)
            }
    }

    fun shutdown() {
        executor.shutdown()
    }
}