package dev.andre.nutritionist.recipe.domain.service

import com.budgetntr.proto.recipe.v1.MealPlanItemProto
import dev.andre.nutritionist.recipe.infrastructure.grpc.client.CatalogClient
import dev.andre.nutritionist.recipe.infrastructure.security.AuthContext
import dev.andre.nutritionist.recipe.persistence.entity.MealPlanEntity
import dev.andre.nutritionist.recipe.persistence.entity.MealPlanItemEntity
import dev.andre.nutritionist.recipe.persistence.repository.KeysetSearchResult
import dev.andre.nutritionist.recipe.persistence.repository.MealPlanRepository
import dev.andre.nutritionist.recipe.persistence.repository.MealPlanSearchCriteria
import dev.andre.nutritionist.recipe.persistence.repository.MealPlanSearchRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

@Service
class MealPlanService(
    private val mealPlanRepository: MealPlanRepository,
    private val mealPlanSearchRepository: MealPlanSearchRepository,
    private val coachingAccessService: CoachingAccessService,
    private val catalogClient: CatalogClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun searchMealPlans(
        query: String?,
        authorType: String?,
        visibility: String?,
        pageSize: Int,
        pageToken: String?,
        authContext: AuthContext,
    ): KeysetSearchResult<MealPlanEntity> {
        val currentUserId = authContext.userId
        val allowedPlanIds =
            currentUserId?.let {
                coachingAccessService.getAllowedTargetIds(it, "MEAL_PLAN")
            } ?: emptySet()

        val parsedAuthorType =
            authorType
                ?.takeIf { it.isNotBlank() && it != "AUTHOR_TYPE_UNSPECIFIED" }
                ?.removePrefix("AUTHOR_TYPE_")

        val parsedVisibility =
            visibility
                ?.takeIf { it.isNotBlank() && it != "CONTENT_VISIBILITY_UNSPECIFIED" }
                ?.removePrefix("CONTENT_VISIBILITY_")

        val criteria =
            MealPlanSearchCriteria(
                query = query,
                authorType = parsedAuthorType,
                visibility = parsedVisibility,
                currentUserId = currentUserId,
                allowedPlanIds = allowedPlanIds,
                pageSize = pageSize,
                pageToken = pageToken,
            )
        return mealPlanSearchRepository.searchMealPlans(criteria)
    }

    /**
     * Возвращает пару: MealPlanEntity и флаг canViewItems (полный доступ к блюдам).
     * Если у пользователя нет полного доступа к платному плану, canViewItems = false (отдаётся превью без блюд).
     */
    @Transactional(readOnly = true)
    fun getMealPlan(
        planId: UUID,
        authContext: AuthContext,
    ): Pair<MealPlanEntity, Boolean> {
        val plan =
            mealPlanRepository.findWithItemsById(planId)
                ?: throw NoSuchElementException("Meal plan with id $planId not found")

        val currentUserId = authContext.userId
        val isAuthor = currentUserId != null && plan.authorId == currentUserId
        val isAdmin = authContext.isAdmin
        val isPublicFree = plan.visibility.equals("PUBLIC", ignoreCase = true) && plan.accessMode.equals("FREE", ignoreCase = true)
        val hasGrant = currentUserId != null && coachingAccessService.hasActiveAccessGrant(currentUserId, "MEAL_PLAN", plan.id)

        val canViewItems = isAuthor || isAdmin || isPublicFree || hasGrant

        // Если план приватный и пользователь не автор/админ/нет гранта — отказ в доступе
        if (plan.visibility.equals("PRIVATE", ignoreCase = true) && !isAuthor && !isAdmin && !hasGrant) {
            throw SecurityException("Access denied to private meal plan $planId")
        }

        return Pair(plan, canViewItems)
    }

    @Transactional
    fun createMealPlan(
        title: String,
        description: String?,
        durationDays: Int,
        visibility: String,
        accessMode: String,
        priceAmountMinor: Long,
        currency: String,
        items: List<MealPlanItemProto>,
        authContext: AuthContext,
    ): MealPlanEntity {
        val currentUserId = checkNotNull(authContext.userId) { "Authentication required to create meal plan" }
        require(title.isNotBlank()) { "Meal plan title must not be blank" }
        require(durationDays > 0) { "durationDays must be greater than zero" }

        val authorType = if (authContext.hasRole(AuthContext.ROLE_COACH)) "COACH" else "USER"
        val cleanVisibility = visibility.removePrefix("CONTENT_VISIBILITY_").ifBlank { "PRIVATE" }
        val cleanAccessMode = accessMode.removePrefix("ACCESS_MODE_").ifBlank { "FREE" }

        val plan =
            MealPlanEntity(
                id = UUID.randomUUID(),
                authorId = currentUserId,
                authorType = authorType,
                title = title,
                description = description,
                durationDays = durationDays,
                visibility = cleanVisibility,
                accessMode = cleanAccessMode,
                priceAmountMinor = priceAmountMinor,
                currency = currency.ifBlank { "RUB" },
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )

        applyItemsAndCalculateDailyAverages(plan, items, durationDays)
        val saved = mealPlanRepository.save(plan)
        logger.info("Created meal plan id={} title='{}' by user={}", saved.id, saved.title, currentUserId)
        return saved
    }

    @Transactional
    fun updateMealPlan(
        planId: UUID,
        title: String,
        description: String?,
        durationDays: Int,
        visibility: String,
        accessMode: String,
        priceAmountMinor: Long,
        currency: String,
        items: List<MealPlanItemProto>,
        authContext: AuthContext,
    ): MealPlanEntity {
        val plan =
            mealPlanRepository.findWithItemsById(planId)
                ?: throw NoSuchElementException("Meal plan with id $planId not found")

        checkWriteAccess(plan, authContext)
        require(title.isNotBlank()) { "Meal plan title must not be blank" }
        val days = if (durationDays > 0) durationDays else plan.durationDays

        plan.title = title
        plan.description = description
        plan.durationDays = days
        plan.visibility = visibility.removePrefix("CONTENT_VISIBILITY_").ifBlank { plan.visibility }
        plan.accessMode = accessMode.removePrefix("ACCESS_MODE_").ifBlank { plan.accessMode }
        plan.priceAmountMinor = priceAmountMinor
        plan.currency = currency.ifBlank { plan.currency }
        plan.updatedAt = Instant.now()

        plan.items.clear()
        applyItemsAndCalculateDailyAverages(plan, items, days)

        val updated = mealPlanRepository.save(plan)
        logger.info("Updated meal plan id={} title='{}'", updated.id, updated.title)
        return updated
    }

    @Transactional
    fun deleteMealPlan(
        planId: UUID,
        authContext: AuthContext,
    ): Boolean {
        val plan =
            mealPlanRepository.findById(planId).orElseThrow {
                NoSuchElementException("Meal plan with id $planId not found")
            }
        checkWriteAccess(plan, authContext)

        mealPlanRepository.delete(plan)
        logger.info("Deleted meal plan id={}", planId)
        return true
    }

    private fun checkWriteAccess(
        plan: MealPlanEntity,
        authContext: AuthContext,
    ) {
        val currentUserId = checkNotNull(authContext.userId) { "Authentication required to modify meal plan" }
        if (plan.authorId == currentUserId || authContext.isAdmin) return
        throw SecurityException("Only plan author or admin can modify this meal plan")
    }

    private fun applyItemsAndCalculateDailyAverages(
        plan: MealPlanEntity,
        items: List<MealPlanItemProto>,
        durationDays: Int,
    ) {
        var totalCalories = BigDecimal.ZERO
        var totalProteins = BigDecimal.ZERO
        var totalFats = BigDecimal.ZERO
        var totalCarbs = BigDecimal.ZERO

        items.forEach { dto ->
            val cal = BigDecimal.valueOf(dto.calories).coerceAtLeast(BigDecimal.ZERO)
            val prot = BigDecimal.valueOf(dto.proteins).coerceAtLeast(BigDecimal.ZERO)
            val fat = BigDecimal.valueOf(dto.fats).coerceAtLeast(BigDecimal.ZERO)
            val carb = BigDecimal.valueOf(dto.carbs).coerceAtLeast(BigDecimal.ZERO)
            val weight = BigDecimal.valueOf(dto.weightGrams).coerceAtLeast(BigDecimal.ZERO)

            totalCalories = totalCalories.add(cal)
            totalProteins = totalProteins.add(prot)
            totalFats = totalFats.add(fat)
            totalCarbs = totalCarbs.add(carb)

            val cleanSlot =
                dto.mealSlot.name
                    .removePrefix("MEAL_TYPE_")
                    .ifBlank { "BREAKFAST" }
            val cleanItemType =
                dto.itemType.name
                    .removePrefix("PLAN_ITEM_TYPE_")
                    .ifBlank { "RECIPE" }

            val item =
                MealPlanItemEntity(
                    id = if (dto.id.isNotBlank()) UUID.fromString(dto.id) else UUID.randomUUID(),
                    plan = plan,
                    dayNumber = dto.dayNumber.coerceAtLeast(1),
                    mealSlot = cleanSlot,
                    itemType = cleanItemType,
                    itemId = if (dto.itemId.isNotBlank()) UUID.fromString(dto.itemId) else UUID.randomUUID(),
                    nameSnapshot = dto.nameSnapshot.ifBlank { "Блюдо" },
                    weightGrams = weight.setScale(2, RoundingMode.HALF_UP),
                    calories = cal.setScale(2, RoundingMode.HALF_UP),
                    proteins = prot.setScale(2, RoundingMode.HALF_UP),
                    fats = fat.setScale(2, RoundingMode.HALF_UP),
                    carbs = carb.setScale(2, RoundingMode.HALF_UP),
                )
            plan.items.add(item)
        }

        val daysFactor = BigDecimal.valueOf(durationDays.toLong().coerceAtLeast(1L))
        plan.avgDailyCalories = totalCalories.divide(daysFactor, 2, RoundingMode.HALF_UP)
        plan.avgDailyProteins = totalProteins.divide(daysFactor, 2, RoundingMode.HALF_UP)
        plan.avgDailyFats = totalFats.divide(daysFactor, 2, RoundingMode.HALF_UP)
        plan.avgDailyCarbs = totalCarbs.divide(daysFactor, 2, RoundingMode.HALF_UP)
    }
}
