package dev.andre.nutritionist.diary.domain.model

data class Money(
    val amountMinor: Long,
    val currency: Currency = Currency.RUB,
)
