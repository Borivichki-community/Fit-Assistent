package dev.andre.nutritionist.domain.repository

import dev.andre.nutritionist.domain.model.DiaryEntryId
import kotlin.test.Test
import kotlin.test.assertTrue

class FakeDiaryRepositoryTest {
    @Test
    fun testFakeRepository() {
        val repo = FakeDiaryRepository()

        repo.deleteEntry(DiaryEntryId("test"))

        assertTrue(repo.invoked, "The repository should record invocation")
    }
}
