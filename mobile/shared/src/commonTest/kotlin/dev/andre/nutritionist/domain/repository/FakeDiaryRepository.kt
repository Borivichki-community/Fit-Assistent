package dev.andre.nutritionist.domain.repository

import dev.andre.nutritionist.domain.model.DiaryEntryId

class FakeDiaryRepository : DiaryRepository {
    var invoked = false

    // We emulate a method for the interface
    fun deleteEntry(id: DiaryEntryId) {
        invoked = true
    }
}
