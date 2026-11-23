package dev.andre.nutritionist.diary.domain.repository

import dev.andre.nutritionist.diary.domain.model.AddEntryCommand
import dev.andre.nutritionist.diary.domain.model.DiaryEntry
import dev.andre.nutritionist.diary.domain.model.DiaryEntryId
import dev.andre.nutritionist.diary.domain.model.UserId
import kotlin.time.Instant

interface DiaryRepository {
    fun add(command: AddEntryCommand): DiaryEntry

    fun delete(
        userId: UserId,
        entryId: DiaryEntryId,
    ): Boolean

    fun getRange(
        userId: UserId,
        from: Instant,
        until: Instant,
    ): List<DiaryEntry>
}
