package dev.andre.nutritionist.profile.service

import com.github.f4b6a3.uuid.UuidCreator
import dev.andre.nutritionist.profile.domain.calculator.NutritionGoalCalculator
import dev.andre.nutritionist.profile.domain.mapper.toEmbeddable
import dev.andre.nutritionist.profile.domain.model.NutritionGoal
import dev.andre.nutritionist.profile.domain.model.UpsertProfileCommand
import dev.andre.nutritionist.profile.persistence.entity.ProfileEntity
import dev.andre.nutritionist.profile.persistence.repository.ProfileJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant

@Service
class ProfileService(
    private val profileRepository: ProfileJpaRepository,
) {
    @Transactional(readOnly = true)
    fun getProfileByUserId(userId: String): ProfileEntity? = profileRepository.findByUserId(userId)

    @Transactional
    fun upsertProfile(command: UpsertProfileCommand): ProfileEntity {
        val existing = profileRepository.findByUserId(command.userId)
        val finalGoal = resolveNutritionGoal(command)

        val entity =
            if (existing != null) {
                existing.heightCm = command.heightCm
                existing.weightKg = command.weightKg
                existing.gender = command.gender
                existing.activityLevel = command.activityLevel
                existing.goalType = command.goalType
                existing.nutritionGoal = finalGoal.toEmbeddable()
                existing.weeklyBudget = command.weeklyBudget
                existing.updatedAt = Instant.now()
                existing
            } else {
                val now = Instant.now()
                ProfileEntity(
                    id = UuidCreator.getTimeOrderedEpoch(),
                    userId = command.userId,
                    heightCm = command.heightCm,
                    weightKg = command.weightKg,
                    gender = command.gender,
                    activityLevel = command.activityLevel,
                    goalType = command.goalType,
                    nutritionGoal = finalGoal.toEmbeddable(),
                    weeklyBudget = command.weeklyBudget,
                    createdAt = now,
                    updatedAt = now,
                )
            }

        return profileRepository.save(entity)
    }

    private fun resolveNutritionGoal(command: UpsertProfileCommand): NutritionGoal {
        val explicitGoal = command.nutritionGoal
        if (explicitGoal != null && explicitGoal.targetCalories > 0) {
            return explicitGoal
        }

        return NutritionGoalCalculator.calculate(
            heightCm = command.heightCm,
            weightKg = command.weightKg,
            gender = command.gender,
            activityLevel = command.activityLevel,
            goalType = command.goalType,
            targetWeightKg = explicitGoal?.targetWeightKg?.takeIf { it > BigDecimal.ZERO },
        )
    }
}
