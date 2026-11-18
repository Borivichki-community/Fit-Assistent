package dev.andre.nutritionist.profile.service

import dev.andre.nutritionist.contracts.domain.Meal
import dev.andre.nutritionist.profile.persistence.repository.ProcessedEventRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.MessageDigest

@Service
class CaloriesConsumptionService(
    private val events: ProcessedEventRepository,
) {
    @Transactional
    fun consume(meal: Meal) {
        require(meal.calories >= 0) { "Calories must be non-negative" }
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { output ->
            output.writeInt(meal.calories)
            output.writeLong(meal.timestamp.toEpochMilliseconds())
            listOf(meal.mealType, meal.notes).forEach { value ->
                output.writeBoolean(value != null)
                value?.let {
                    val encoded = it.toByteArray(Charsets.UTF_8)
                    output.writeInt(encoded.size)
                    output.write(encoded)
                }
            }
        }
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()).toHexString()
        require(events.insert(meal, hash) || events.matches(meal, hash)) {
            "Conflicting CaloriesConsumedEvent for the same user and meal"
        }
    }
}
