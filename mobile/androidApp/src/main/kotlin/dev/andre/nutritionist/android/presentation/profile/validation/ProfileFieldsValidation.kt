package dev.andre.nutritionist.android.presentation.profile.validation

import dev.andre.nutritionist.android.R

/**
 * Ошибки валидации полей ввода профиля пользователя.
 */
sealed interface ProfileValidationError {
    data object Empty : ProfileValidationError
    data object NotANumber : ProfileValidationError
    data object NegativeOrZero : ProfileValidationError
    data class OutOfRange(val min: Number, val max: Number) : ProfileValidationError
    data class TooShort(val minLength: Int) : ProfileValidationError
}

/**
 * Чистые функции валидации полей профиля.
 */
object ProfileFieldsValidation {

    fun validateName(name: String): ProfileValidationError? {
        if (name.isBlank()) {
            return ProfileValidationError.Empty
        }
        if (name.trim().length < 2) {
            return ProfileValidationError.TooShort(minLength = 2)
        }
        return null
    }

    fun validateAge(age: String): ProfileValidationError? {
        if (age.isBlank()) {
            return ProfileValidationError.Empty
        }
        val intValue = age.toIntOrNull() ?: return ProfileValidationError.NotANumber
        if (intValue <= 0) {
            return ProfileValidationError.NegativeOrZero
        }
        if (intValue !in 14..120) {
            return ProfileValidationError.OutOfRange(min = 14, max = 120)
        }
        return null
    }

    fun validateTargetWeight(targetWeight: String): ProfileValidationError? {
        if (targetWeight.isBlank()) {
            return ProfileValidationError.Empty
        }
        val doubleValue = targetWeight.toDoubleOrNull() ?: return ProfileValidationError.NotANumber
        if (doubleValue <= 0.0) {
            return ProfileValidationError.NegativeOrZero
        }
        if (doubleValue !in 30.0..300.0) {
            return ProfileValidationError.OutOfRange(min = 30.0, max = 300.0)
        }
        return null
    }

    fun validateDailyCalories(dailyCalories: String): ProfileValidationError? {
        if (dailyCalories.isBlank()) {
            return ProfileValidationError.Empty
        }
        val intValue = dailyCalories.toIntOrNull() ?: return ProfileValidationError.NotANumber
        if (intValue <= 0) {
            return ProfileValidationError.NegativeOrZero
        }
        if (intValue !in 500..10000) {
            return ProfileValidationError.OutOfRange(min = 500, max = 10000)
        }
        return null
    }

    fun toErrorMessageResId(error: ProfileValidationError): Int = when (error) {
        is ProfileValidationError.Empty -> R.string.validation_error_empty
        is ProfileValidationError.NotANumber -> R.string.validation_error_not_a_number
        is ProfileValidationError.NegativeOrZero -> R.string.validation_error_negative_or_zero
        is ProfileValidationError.OutOfRange -> R.string.validation_error_out_of_range
        is ProfileValidationError.TooShort -> R.string.validation_error_too_short
    }
}
