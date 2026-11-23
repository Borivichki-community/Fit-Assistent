package dev.andre.nutritionist.domain.validation

import dev.andre.nutritionist.domain.model.OnboardingData

/**
 * Единый KMP-валидатор профиля пользователя и онбординга.
 * Синхронизирован с серверными константами валидации ProfileRequestValidator (gRPC transport).
 */
object ProfileValidator {

    private const val MIN_NAME_LENGTH = 2
    private val HEIGHT_RANGE = 50..300
    private val WEIGHT_RANGE = 20.0..500.0
    private val AGE_RANGE = 14..120
    private val BUDGET_RANGE = 1.0..1_000_000.0
    private val CALORIES_RANGE = 500..10_000

    fun validateName(raw: String): FieldValidationError? {
        if (raw.isBlank()) {
            return FieldValidationError.Empty
        }
        if (raw.trim().length < MIN_NAME_LENGTH) {
            return FieldValidationError.TooShort(minLength = MIN_NAME_LENGTH)
        }
        return null
    }

    fun validateHeight(raw: String): FieldValidationError? {
        if (raw.isBlank()) {
            return FieldValidationError.Empty
        }
        val intValue = raw.trim().toIntOrNull() ?: return FieldValidationError.NotANumber
        if (intValue <= 0) {
            return FieldValidationError.NegativeOrZero
        }
        if (intValue !in HEIGHT_RANGE) {
            return FieldValidationError.OutOfRange(min = HEIGHT_RANGE.first, max = HEIGHT_RANGE.last)
        }
        return null
    }

    fun validateWeight(raw: String): FieldValidationError? {
        if (raw.isBlank()) {
            return FieldValidationError.Empty
        }
        val cleaned = raw.trim().replace(',', '.')
        val doubleValue = cleaned.toDoubleOrNull() ?: return FieldValidationError.NotANumber
        if (doubleValue <= 0.0) {
            return FieldValidationError.NegativeOrZero
        }
        if (doubleValue !in WEIGHT_RANGE) {
            return FieldValidationError.OutOfRange(min = WEIGHT_RANGE.start, max = WEIGHT_RANGE.endInclusive)
        }
        return null
    }

    fun validateTargetWeight(raw: String): FieldValidationError? = validateWeight(raw)

    fun validateAge(raw: String): FieldValidationError? {
        if (raw.isBlank()) {
            return FieldValidationError.Empty
        }
        val intValue = raw.trim().toIntOrNull() ?: return FieldValidationError.NotANumber
        if (intValue <= 0) {
            return FieldValidationError.NegativeOrZero
        }
        if (intValue !in AGE_RANGE) {
            return FieldValidationError.OutOfRange(min = AGE_RANGE.first, max = AGE_RANGE.last)
        }
        return null
    }

    fun validateWeeklyBudget(raw: String): FieldValidationError? {
        if (raw.isBlank()) {
            return FieldValidationError.Empty
        }
        val cleaned = raw.trim().replace(',', '.')
        val doubleValue = cleaned.toDoubleOrNull() ?: return FieldValidationError.NotANumber
        if (doubleValue <= 0.0) {
            return FieldValidationError.NegativeOrZero
        }
        if (doubleValue !in BUDGET_RANGE) {
            return FieldValidationError.OutOfRange(min = BUDGET_RANGE.start, max = BUDGET_RANGE.endInclusive)
        }
        return null
    }

    fun validateDailyCalories(raw: String): FieldValidationError? {
        if (raw.isBlank()) {
            return FieldValidationError.Empty
        }
        val intValue = raw.trim().toIntOrNull() ?: return FieldValidationError.NotANumber
        if (intValue <= 0) {
            return FieldValidationError.NegativeOrZero
        }
        if (intValue !in CALORIES_RANGE) {
            return FieldValidationError.OutOfRange(min = CALORIES_RANGE.first, max = CALORIES_RANGE.last)
        }
        return null
    }

    data class OnboardingValidationResult(
        val isValid: Boolean,
        val nameError: FieldValidationError? = null,
        val heightError: FieldValidationError? = null,
        val weightError: FieldValidationError? = null,
        val ageError: FieldValidationError? = null,
        val budgetError: FieldValidationError? = null,
    )

    fun validateOnboarding(data: OnboardingData): OnboardingValidationResult {
        val nameError = validateName(data.firstName)
        val heightError = validateHeight(data.heightCm)
        val weightError = validateWeight(data.weightKg)
        val ageError = validateAge(data.age)
        val budgetError = validateWeeklyBudget(data.weeklyBudget)

        val isValid = nameError == null &&
            heightError == null &&
            weightError == null &&
            ageError == null &&
            budgetError == null

        return OnboardingValidationResult(
            isValid = isValid,
            nameError = nameError,
            heightError = heightError,
            weightError = weightError,
            ageError = ageError,
            budgetError = budgetError,
        )
    }
}
