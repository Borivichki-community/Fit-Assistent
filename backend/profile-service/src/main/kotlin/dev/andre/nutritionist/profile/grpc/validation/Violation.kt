package dev.andre.nutritionist.profile.grpc.validation

/** Стабильные машиночитаемые коды нарушений валидации. */
enum class ViolationCode {
    REQUIRED,
    OUT_OF_RANGE,
    NOT_FINITE,
}

data class Violation(
    val field: String,
    val code: ViolationCode,
    val message: String,
    val rejectedValue: Any? = null,
)

/**
 * Накопитель нарушений валидации.
 * Использует List, гарантируя стабильный детерминированный порядок нарушений.
 */
class Violations {
    private val items = mutableListOf<Violation>()

    fun notBlank(field: String, value: String) {
        if (value.isBlank()) {
            items += Violation(field, ViolationCode.REQUIRED, "$field must not be blank")
        }
    }

    fun intInRange(field: String, value: Int, range: IntRange) {
        if (value !in range) {
            items +=
                Violation(
                    field,
                    ViolationCode.OUT_OF_RANGE,
                    "$field must be in ${range.first}..${range.last}",
                    value,
                )
        }
    }

    /**
     * Проверка чисел с плавающей точкой:
     * NaN проходит стандартные проверки вида `<= 0.0` и `< 0.0` (они дают false),
     * поэтому конечность проверяется первой и отдельно.
     */
    fun doubleInRange(field: String, value: Double, range: ClosedFloatingPointRange<Double>) {
        when {
            !value.isFinite() ->
                items +=
                    Violation(
                        field,
                        ViolationCode.NOT_FINITE,
                        "$field must be a finite number",
                        value,
                    )

            value !in range ->
                items +=
                    Violation(
                        field,
                        ViolationCode.OUT_OF_RANGE,
                        "$field must be in ${range.start}..${range.endInclusive}",
                        value,
                    )
        }
    }

    fun build(): List<Violation> = items.toList()
}
