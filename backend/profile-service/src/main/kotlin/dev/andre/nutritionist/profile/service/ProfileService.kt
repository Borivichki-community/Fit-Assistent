package dev.andre.nutritionist.profile.service

import dev.andre.nutritionist.profile.domain.calculator.NutritionGoalCalculator
import dev.andre.nutritionist.profile.domain.mapper.toNewEntity
import dev.andre.nutritionist.profile.domain.mapper.updateFrom
import dev.andre.nutritionist.profile.domain.model.NutritionGoal
import dev.andre.nutritionist.profile.domain.model.UpsertProfileCommand
import dev.andre.nutritionist.profile.domain.model.UserId
import dev.andre.nutritionist.profile.persistence.entity.ProfileEntity
import dev.andre.nutritionist.profile.persistence.repository.ProfileJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class ProfileService(
    private val profileRepository: ProfileJpaRepository,
) {
    @Transactional(readOnly = true)
    fun getProfileByUserId(userId: UserId): ProfileEntity? = profileRepository.findByUserId(userId.raw)

    @Transactional
    fun upsertProfile(command: UpsertProfileCommand): ProfileEntity {
        val existing = profileRepository.findByUserId(command.userId.raw)
        val finalGoal = resolveNutritionGoal(command)
        val entity = existing?.updateFrom(command, finalGoal) ?: command.toNewEntity(finalGoal)
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
