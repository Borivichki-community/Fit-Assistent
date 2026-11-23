package dev.andre.nutritionist.diary.persistence.repository

import dev.andre.nutritionist.diary.domain.model.AddEntryCommand
import dev.andre.nutritionist.diary.domain.model.DiaryEntry
import dev.andre.nutritionist.diary.domain.model.DiaryEntryId
import dev.andre.nutritionist.diary.domain.model.UserId
import dev.andre.nutritionist.diary.domain.repository.DiaryRepository
import dev.andre.nutritionist.diary.persistence.mapper.DiaryEntityMapper
import org.springframework.stereotype.Repository
import kotlin.time.Instant

@Repository
class DiaryRepositoryImpl(
    private val jpaRepository: DiaryJpaRepository,
    private val mapper: DiaryEntityMapper,
) : DiaryRepository {
    override fun add(command: AddEntryCommand): DiaryEntry = mapper.toDomain(jpaRepository.insert(mapper.toEntity(command)))

    override fun delete(
        userId: UserId,
        entryId: DiaryEntryId,
    ): Boolean = jpaRepository.delete(userId, entryId)

    override fun getRange(
        userId: UserId,
        from: Instant,
        until: Instant,
    ): List<DiaryEntry> = jpaRepository.getRange(userId, from, until).map(mapper::toDomain)
}
