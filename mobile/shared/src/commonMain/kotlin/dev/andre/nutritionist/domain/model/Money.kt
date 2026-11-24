package dev.andre.nutritionist.domain.model

import kotlin.jvm.JvmInline
import kotlin.math.roundToLong

/**
 * Деньги в минимальных единицах (копейках).
 * Гарантирует точность финансовых расчётов без ошибок округления с плавающей точкой.
 */
@JvmInline
value class Money(val amountMinor: Long) : Comparable<Money> {

    fun toRubles(): Double = amountMinor / 100.0

    operator fun plus(other: Money): Money = Money(amountMinor + other.amountMinor)

    operator fun minus(other: Money): Money = Money(amountMinor - other.amountMinor)

    operator fun times(factor: Double): Money = Money((amountMinor * factor).roundToLong())

    operator fun div(divisor: Long): Money {
        require(divisor != 0L) { "Divisor cannot be zero" }
        return Money(amountMinor / divisor)
    }

    override fun compareTo(other: Money): Int = amountMinor.compareTo(other.amountMinor)

    companion object {
        const val CURRENCY_RUB: String = "RUB"
        val ZERO: Money = Money(0L)

        fun fromMinor(amountMinor: Long): Money = Money(amountMinor)

        fun fromRubles(rubles: Double): Money {
            require(!rubles.isNaN()) { "Rubles must not be NaN" }
            return Money((rubles * 100.0).roundToLong())
        }
    }
}
