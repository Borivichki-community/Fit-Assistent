package dev.andre.nutritionist.domain.validation

/**
 * Типизированные ошибки валидации полей ввода доменных сущностей и профиля.
 * Чистая KMP-модель без зависимостей от платформенных SDK.
 */
sealed interface FieldValidationError {
    data object Empty : FieldValidationError
    data object NotANumber : FieldValidationError
    data object NegativeOrZero : FieldValidationError
    data class OutOfRange(val min: Number, val max: Number) : FieldValidationError
    data class TooShort(val minLength: Int) : FieldValidationError
}
