package dev.andre.nutritionist.recipe.config

import dev.andre.nutritionist.recipe.domain.model.MenuRequest
import dev.andre.nutritionist.recipe.domain.service.RecipeMenuService
import dev.andre.nutritionist.recipe.infrastructure.llm.QuotaExceeded
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class RecipeWarmup(
    private val service: RecipeMenuService,
    settings: RecipeProperties,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val targets =
        settings.warmupTargets.distinct().map { pair ->
            val parts = pair.split(':')
            require(parts.size == 2) { "recipe.warmup-targets entries must use calories:budgetMinor" }
            MenuRequest(parts[0].toInt(), parts[1].toLong())
        }

    @Scheduled(initialDelayString = "\${recipe.warmup-initial-delay:PT30S}", fixedDelayString = "\${recipe.warmup-interval:PT30M}")
    suspend fun warmup() {
        for (target in targets) {
            try {
                service.warmup(target)
            } catch (error: Exception) {
                if (!RecipeMenuService.isDependencyFailure(error) && error !is QuotaExceeded) throw error
                logger.warn("Recipe cache warmup skipped for {} kcal and {} minor units", target.dailyCalories, target.weeklyBudgetMinor)
            }
        }
    }
}
