package dev.andre.nutritionist.android.presentation.profile.validation

import androidx.annotation.StringRes
import dev.andre.nutritionist.android.R
import dev.andre.nutritionist.domain.validation.FieldValidationError

/**
 * Преобразование доменных ошибок валидации полей ввода в строковые ресурсы Android.
 * Исчерпывающий when без ветки else в соответствии с AGENTS.md §1.2.
 */
@StringRes
fun FieldValidationError.toMessageRes(): Int = when (this) {
    is FieldValidationError.Empty -> R.string.validation_error_empty
    is FieldValidationError.NotANumber -> R.string.validation_error_not_a_number
    is FieldValidationError.NegativeOrZero -> R.string.validation_error_negative_or_zero
    is FieldValidationError.OutOfRange -> R.string.validation_error_out_of_range
    is FieldValidationError.TooShort -> R.string.validation_error_too_short
}
