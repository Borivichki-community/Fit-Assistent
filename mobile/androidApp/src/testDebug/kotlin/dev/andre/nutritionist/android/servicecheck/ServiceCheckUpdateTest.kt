package dev.andre.nutritionist.android.servicecheck

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ServiceCheckUpdateTest {
    private val update = ServiceCheckUpdate()
    private val entry = DiaryCheckEntry.create(Instant.parse("2026-11-21T23:59:59Z"))

    @Test
    fun retryAfterFailureKeepsRequestAndIdempotencyKey() {
        val initial = ServiceCheckState(ServiceCheckTarget.DIARY, entry = entry)
        val add = ServiceCheckEvent.Ui.Run(ServiceCheckAction.ADD_ENTRY)
        val first = update.update(initial, add)

        val busy = checkNotNull(first.state)
        assertTrue(update.update(busy, add).commands.isEmpty())
        val failed = update.update(
            busy,
            ServiceCheckEvent.Internal.Failed(ServiceCheckAction.ADD_ENTRY, "DEADLINE_EXCEEDED"),
        )
        val retry = update.update(checkNotNull(failed.state), add)

        assertEquals(first.commands.single(), retry.commands.single())
        assertEquals(entry.request, retry.commands.single().entry?.request)
        assertEquals(entry.addKey, retry.commands.single().entry?.addKey)
        assertNotEquals(entry.addKey, entry.deleteKey)
    }

    @Test
    fun deletedEntryCanBeRetriedForDeletionButCannotBeAddedAgain() {
        val initial = ServiceCheckState(ServiceCheckTarget.DIARY, entry = entry)
        val delete = ServiceCheckEvent.Ui.Run(ServiceCheckAction.DELETE_ENTRY)
        val first = update.update(initial, delete)
        val completed = update.update(
            checkNotNull(first.state),
            ServiceCheckEvent.Internal.Completed(
                ServiceCheckAction.DELETE_ENTRY,
                "OK",
                entry.copy(deleted = true),
            ),
        )
        val state = checkNotNull(completed.state)

        assertFalse(state.canRun(ServiceCheckAction.ADD_ENTRY))
        assertTrue(state.canRun(ServiceCheckAction.PREPARE_ENTRY))
        assertEquals(entry.deleteKey, update.update(state, delete).commands.single().entry?.deleteKey)
    }
}
