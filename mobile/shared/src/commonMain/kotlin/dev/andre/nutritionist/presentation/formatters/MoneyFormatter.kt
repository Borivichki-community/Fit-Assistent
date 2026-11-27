package dev.andre.nutritionist.presentation.formatters

import dev.andre.nutritionist.domain.model.Money

/**
 * Чистый KMP-форматтер денежных значений (без java.text.NumberFormat и String.format).
 */
object MoneyFormatter {

    fun format(money: Money): String =
        formatWithCurrency(amountMinor = money.amountMinor, currency = Money.CURRENCY_RUB)

    fun formatWithCurrency(amountMinor: Long, currency: String): String {
        val symbol = currencySymbol(currency)
        val isNegative = amountMinor < 0L
        val absMinor = if (amountMinor == Long.MIN_VALUE) {
            Long.MAX_VALUE
        } else if (isNegative) {
            -amountMinor
        } else {
            amountMinor
        }

        val whole = absMinor / 100L
        val fraction = absMinor % 100L
        val wholeFormatted = formatWithThousandsSeparator(whole)
        val signPrefix = if (isNegative) "-" else ""

        return if (fraction == 0L) {
            "$signPrefix$wholeFormatted $symbol"
        } else {
            val fractionFormatted = fraction.toString().padStart(2, '0')
            "$signPrefix$wholeFormatted,$fractionFormatted $symbol"
        }
    }

    private fun currencySymbol(currency: String): String =
        when (val normalized = currency.trim().uppercase()) {
            "RUB", "RUR" -> "₽"
            "USD" -> "$"
            "EUR" -> "€"
            "KZT" -> "₸"
            "BYN" -> "Br"
            "" -> "₽"
            else -> normalized
        }

    internal fun formatWithThousandsSeparator(value: Long): String {
        val digits = value.toString()
        if (digits.length <= 3) return digits
        return buildString {
            val firstGroupLen = digits.length % 3
            var index = 0
            if (firstGroupLen > 0) {
                append(digits, 0, firstGroupLen)
                index = firstGroupLen
                append(' ')
            }
            while (index < digits.length) {
                append(digits, index, index + 3)
                index += 3
                if (index < digits.length) append(' ')
            }
        }
    }
}
