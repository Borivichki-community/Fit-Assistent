package dev.andre.nutritionist.data.local

import dev.andre.nutritionist.data.local.dao.MealRecordDao
import dev.andre.nutritionist.data.local.entity.MealRecordEntity
import dev.andre.nutritionist.domain.coroutines.CoroutineDispatchers
import dev.andre.nutritionist.domain.model.EntrySourceType
import dev.andre.nutritionist.domain.model.MealType
import dev.andre.nutritionist.domain.model.SyncStatus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * Идемпотентный инициализатор стартовой записи дневника питания в локальной Room БД.
 */
interface DiaryInitialSeeder {
    /**
     * Создаёт стартовую запись «Йогурт Epica» на текущий день, если таблица `meal_records`
     * ещё ни разу не содержала записей (включая удалённые tombstone-записи).
     */
    suspend fun ensureSeeded()
}

/**
 * Реализация [DiaryInitialSeeder] поверх [MealRecordDao].
 * Повторный вызов или удаление записи пользователем не приводит к дублированию/восстановлению.
 */
class RoomDiaryInitialSeeder(
    private val mealRecordDao: MealRecordDao,
    private val dispatchers: CoroutineDispatchers,
    private val nowProvider: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : DiaryInitialSeeder {

    private val seedMutex = Mutex()
    // mutable by design: prevents re-seeding within the session after permanent deletion
    private var hasAttemptedSeed = false

    override suspend fun ensureSeeded() {
        withContext(dispatchers.io) {
            seedMutex.withLock {
                if (hasAttemptedSeed) return@withLock
                if (mealRecordDao.countAllIncludingDeleted() == 0) {
                    mealRecordDao.insert(
                        MealRecordEntity(
                            id = EPICA_RECORD_ID,
                            idempotencyKey = EPICA_RECORD_ID,
                            syncStatus = SyncStatus.SYNCED.name,
                            mealType = MealType.LUNCH.name,
                            recordedAtEpoch = nowProvider(),
                            sourceType = EntrySourceType.PRODUCT.name,
                            sourceId = EPICA_PRODUCT_ID,
                            nameSnapshot = EPICA_NAME,
                            weightGrams = EPICA_WEIGHT_GRAMS,
                            calories = EPICA_CALORIES,
                            proteins = EPICA_PROTEINS,
                            fats = EPICA_FATS,
                            carbs = EPICA_CARBS,
                            costAmountMinor = EPICA_PRICE_MINOR,
                            costCurrency = "RUB",
                            isDeleted = false,
                        ),
                    )
                }
                hasAttemptedSeed = true
            }
        }
    }

    companion object {
        const val EPICA_RECORD_ID: String = "seed-epica-lunch-1"
        const val EPICA_PRODUCT_ID: String = "20000000-0000-0000-0000-000000000112"
        const val EPICA_NAME: String = "Йогурт Epica"
        const val EPICA_WEIGHT_GRAMS: Double = 130.0
        const val EPICA_CALORIES: Double = 85.0
        const val EPICA_PROTEINS: Double = 4.93
        const val EPICA_FATS: Double = 1.25
        const val EPICA_CARBS: Double = 13.8
        const val EPICA_PRICE_MINOR: Long = 9500L
    }
}
