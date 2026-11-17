package dev.andre.nutritionist.profile.service

import dev.andre.nutritionist.profile.domain.model.DailyCaloriesBalance
import dev.andre.nutritionist.profile.domain.model.UserId
import dev.andre.nutritionist.profile.persistence.repository.ProcessedEventRepository
import dev.andre.nutritionist.profile.persistence.repository.ProfileJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

@Service
class DailyCaloriesService(
    private val profiles: ProfileJpaRepository,
    private val events: ProcessedEventRepository,
) {
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    fun getBalance(
        userId: UserId,
        date: LocalDate,
        zone: ZoneId,
    ): DailyCaloriesBalance {
        val profile = profiles.findByUserId(userId.raw) ?: throw NoSuchElementException("Profile not found")
        val consumed =
            events.sum(
                userId,
                date.atStartOfDay(zone).toInstant(),
                date.plusDays(1).atStartOfDay(zone).toInstant(),
            )
        val target = profile.nutritionGoal.targetCalories
        return DailyCaloriesBalance(target, consumed, Math.subtractExact(target.toLong(), consumed))
    }
}
