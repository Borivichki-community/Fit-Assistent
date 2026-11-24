package dev.andre.nutritionist.profile.service

import dev.andre.nutritionist.contracts.avro.UserRegisteredEvent
import dev.andre.nutritionist.profile.domain.calculator.NutritionGoalCalculator
import dev.andre.nutritionist.profile.domain.mapper.toNewEntity
import dev.andre.nutritionist.profile.domain.mapper.updateFrom
import dev.andre.nutritionist.profile.domain.model.NutritionGoal
import dev.andre.nutritionist.profile.domain.model.UpsertProfileCommand
import dev.andre.nutritionist.profile.domain.model.UserId
import dev.andre.nutritionist.profile.outbox.processor.OutboxProcessor.Companion.USER_REGISTERED_TOPIC
import dev.andre.nutritionist.profile.outbox.service.OutboxPayloadConverter
import dev.andre.nutritionist.profile.outbox.service.OutboxService
import dev.andre.nutritionist.profile.persistence.entity.ProfileEntity
import dev.andre.nutritionist.profile.persistence.repository.ProfileJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
class ProfileService(
    private val profileRepository: ProfileJpaRepository,
    private val outboxService: OutboxService,
) {
    private val logger = LoggerFactory.getLogger(ProfileService::class.java)

    @Transactional(readOnly = true)
    fun getProfileByUserId(userId: UserId): ProfileEntity? = profileRepository.findByUserId(userId.raw)

    @Transactional
    fun upsertProfile(command: UpsertProfileCommand): ProfileEntity {
        val existing = profileRepository.findByUserId(command.userId.raw)
        val isFilledCommand = command.heightCm > 0 && command.weightKg > BigDecimal.ZERO
        val wasIncomplete = existing == null || (existing.heightCm <= 0 || existing.weightKg <= BigDecimal.ZERO)
        val isNewProfile = isFilledCommand && wasIncomplete

        val finalGoal = resolveNutritionGoal(command)
        val entity = existing?.updateFrom(command, finalGoal) ?: command.toNewEntity(finalGoal)
        val saved = profileRepository.save(entity)

        if (isNewProfile) {
            logger.info("Scheduling UserRegisteredEvent to outbox for userId={}", entity.userId)
            publishUserRegisteredOutbox(saved, command)
        } else {
            logger.debug("Skipping UserRegisteredEvent: profile already exists for userId={}", command.userId.raw)
        }

        return saved
    }

    private fun publishUserRegisteredOutbox(
        entity: ProfileEntity,
        command: UpsertProfileCommand,
    ) {
        val userUuid =
            runCatching { UUID.fromString(command.userId.raw) }
                .getOrElse { entity.id }

        val email = command.email ?: "${command.userId.raw}@budgetntr.app"

        val event =
            UserRegisteredEvent
                .newBuilder()
                .setId(userUuid)
                .setEmail(email)
                .setRegistrationTimestamp(Instant.now())
                .setSource("profile-service")
                .build()

        val payload = OutboxPayloadConverter.serialize(event)

        outboxService.save(
            aggregateType = "UserProfile",
            aggregateId = entity.userId,
            topic = USER_REGISTERED_TOPIC,
            payload = payload,
        )
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
