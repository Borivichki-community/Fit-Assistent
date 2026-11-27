package dev.andre.nutritionist.fake

import dev.andre.nutritionist.domain.sync.SyncScheduler

class FakeSyncScheduler : SyncScheduler {

    // mutable by design: test call verification
    var scheduleCallCount: Int = 0
        private set

    override fun scheduleDiarySync() {
        scheduleCallCount++
    }

    fun reset() {
        scheduleCallCount = 0
    }
}
