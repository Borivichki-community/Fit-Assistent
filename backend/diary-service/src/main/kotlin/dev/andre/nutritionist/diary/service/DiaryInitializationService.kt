package dev.andre.nutritionist.diary.service

import dev.andre.nutritionist.diary.domain.model.UserId
import dev.andre.nutritionist.diary.persistence.repository.DiaryInitializationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.time.Instant

@Service
class DiaryInitializationService(
    private val repository: DiaryInitializationRepository,
) {
    @Transactional
    fun initialize(
        userId: UserId,
        registeredAt: Instant,
    ): Boolean {
        if (!repository.markProcessed(userId)) {
            return false
        }
        repository.initialize(userId, registeredAt)
        return true
    }
}
