package dev.andre.nutritionist.diary.service

import dev.andre.nutritionist.diary.domain.model.AddEntryCommand
import dev.andre.nutritionist.diary.domain.model.DailyEntries
import dev.andre.nutritionist.diary.domain.model.DiaryEntry
import dev.andre.nutritionist.diary.domain.model.DiaryEntryId
import dev.andre.nutritionist.diary.domain.model.NutritionSummary
import dev.andre.nutritionist.diary.domain.model.UserId
import dev.andre.nutritionist.diary.domain.repository.DiaryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import kotlin.time.Instant

@Service
class DiaryService(
    private val repository: DiaryRepository,
) {
    @Transactional(propagation = Propagation.MANDATORY)
    fun addEntry(command: AddEntryCommand): DiaryEntry {
        require(command.foodName.isNotBlank() && command.foodName.length <= 255) { "Invalid food_name" }
        require(command.quantityGrams.isFinite() && command.quantityGrams > 0 && command.quantityGrams <= 9_999_999.999) {
            "Invalid quantity_grams"
        }
        require(command.nutrition.calories in 0..99_999) { "Invalid calories" }
        listOf(command.nutrition.proteins, command.nutrition.fats, command.nutrition.carbs).forEach {
            require(it.isFinite() && it in 0.0..9_999.99) { "Invalid nutrition value" }
        }
        command.cost?.let { cost ->
            require(cost.total.amountMinor in 0..9_999_999_999L) { "Invalid total cost" }
            require((cost.unitPrice == null) == (cost.priceBasisMilligrams == null)) { "Incomplete price basis" }
            cost.unitPrice?.let { require(it.amountMinor >= 0) { "Invalid unit price" } }
            cost.priceBasisMilligrams?.let { require(it > 0) { "Invalid price basis" } }
        }
        return repository.add(command)
    }

    @Transactional(propagation = Propagation.MANDATORY)
    fun deleteMeal(
        userId: UserId,
        entryId: DiaryEntryId,
    ) {
        if (!repository.delete(userId, entryId)) {
            throw NoSuchElementException("Diary entry not found")
        }
    }

    @Transactional(readOnly = true)
    fun getDay(
        userId: UserId,
        from: Instant,
        until: Instant,
    ): DailyEntries {
        val entries = getRange(userId, from, until)
        return DailyEntries(
            entries,
            NutritionSummary(
                entries.fold(0) { total, entry -> Math.addExact(total, entry.nutrition.calories) },
                entries.sumOf { it.nutrition.proteins },
                entries.sumOf { it.nutrition.fats },
                entries.sumOf { it.nutrition.carbs },
            ),
        )
    }

    @Transactional(readOnly = true)
    fun getRange(
        userId: UserId,
        from: Instant,
        until: Instant,
    ): List<DiaryEntry> {
        require(from < until) { "Invalid date range" }
        return repository.getRange(userId, from, until)
    }
}
