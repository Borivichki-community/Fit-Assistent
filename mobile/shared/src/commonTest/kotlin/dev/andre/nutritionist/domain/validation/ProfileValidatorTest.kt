package dev.andre.nutritionist.domain.validation

import dev.andre.nutritionist.domain.model.ActivityLevel
import dev.andre.nutritionist.domain.model.Gender
import dev.andre.nutritionist.domain.model.GoalType
import dev.andre.nutritionist.domain.model.OnboardingData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileValidatorTest {

    // =========================================================================
    // Валидация имени (validateName)
    // =========================================================================

    @Test
    fun `validateName returns Empty for empty or blank input`() {
        assertEquals(FieldValidationError.Empty, ProfileValidator.validateName(""))
        assertEquals(FieldValidationError.Empty, ProfileValidator.validateName("   "))
        assertEquals(FieldValidationError.Empty, ProfileValidator.validateName("\t\n"))
    }

    @Test
    fun `validateName returns TooShort for single character input`() {
        assertEquals(FieldValidationError.TooShort(minLength = 2), ProfileValidator.validateName("A"))
        assertEquals(FieldValidationError.TooShort(minLength = 2), ProfileValidator.validateName(" Б "))
    }

    @Test
    fun `validateName returns null for valid name of 2 or more characters`() {
        assertNull(ProfileValidator.validateName("Al"))
        assertNull(ProfileValidator.validateName("Aleksey"))
        assertNull(ProfileValidator.validateName("Иван Иванов"))
    }

    // =========================================================================
    // Валидация роста (validateHeight: 50 .. 300 см)
    // =========================================================================

    @Test
    fun `validateHeight returns Empty for empty or blank input`() {
        assertEquals(FieldValidationError.Empty, ProfileValidator.validateHeight(""))
        assertEquals(FieldValidationError.Empty, ProfileValidator.validateHeight("  "))
    }

    @Test
    fun `validateHeight returns NotANumber for non-integer inputs`() {
        assertEquals(FieldValidationError.NotANumber, ProfileValidator.validateHeight("abc"))
        assertEquals(FieldValidationError.NotANumber, ProfileValidator.validateHeight("175.5"))
    }

    @Test
    fun `validateHeight returns NegativeOrZero for non-positive numbers`() {
        assertEquals(FieldValidationError.NegativeOrZero, ProfileValidator.validateHeight("0"))
        assertEquals(FieldValidationError.NegativeOrZero, ProfileValidator.validateHeight("-10"))
    }

    @Test
    fun `validateHeight returns OutOfRange for values below 50 or above 300`() {
        assertEquals(FieldValidationError.OutOfRange(min = 50, max = 300), ProfileValidator.validateHeight("40"))
        assertEquals(FieldValidationError.OutOfRange(min = 50, max = 300), ProfileValidator.validateHeight("49"))
        assertEquals(FieldValidationError.OutOfRange(min = 50, max = 300), ProfileValidator.validateHeight("301"))
        assertEquals(FieldValidationError.OutOfRange(min = 50, max = 300), ProfileValidator.validateHeight("350"))
    }

    @Test
    fun `validateHeight returns null for boundary and valid heights`() {
        assertNull(ProfileValidator.validateHeight("50"))
        assertNull(ProfileValidator.validateHeight("180"))
        assertNull(ProfileValidator.validateHeight("300"))
    }

    // =========================================================================
    // Валидация веса (validateWeight: 20.0 .. 500.0 кг)
    // =========================================================================

    @Test
    fun `validateWeight returns Empty for empty or blank input`() {
        assertEquals(FieldValidationError.Empty, ProfileValidator.validateWeight(""))
        assertEquals(FieldValidationError.Empty, ProfileValidator.validateWeight("  "))
    }

    @Test
    fun `validateWeight returns NotANumber for letters and non-number strings`() {
        assertEquals(FieldValidationError.NotANumber, ProfileValidator.validateWeight("abc"))
        assertEquals(FieldValidationError.NotANumber, ProfileValidator.validateWeight("75.a"))
    }

    @Test
    fun `validateWeight returns NegativeOrZero for zero or negative values`() {
        assertEquals(FieldValidationError.NegativeOrZero, ProfileValidator.validateWeight("0"))
        assertEquals(FieldValidationError.NegativeOrZero, ProfileValidator.validateWeight("0.0"))
        assertEquals(FieldValidationError.NegativeOrZero, ProfileValidator.validateWeight("-70.0"))
        assertEquals(FieldValidationError.NegativeOrZero, ProfileValidator.validateWeight("-1"))
    }

    @Test
    fun `validateWeight returns OutOfRange for values below 20_0 or above 500_0`() {
        assertEquals(FieldValidationError.OutOfRange(min = 20.0, max = 500.0), ProfileValidator.validateWeight("19.9"))
        assertEquals(FieldValidationError.OutOfRange(min = 20.0, max = 500.0), ProfileValidator.validateWeight("500.1"))
        assertEquals(FieldValidationError.OutOfRange(min = 20.0, max = 500.0), ProfileValidator.validateWeight("600.0"))
    }

    @Test
    fun `validateWeight returns null for boundary values and decimal formats`() {
        assertNull(ProfileValidator.validateWeight("20.0"))
        assertNull(ProfileValidator.validateWeight("75.5"))
        assertNull(ProfileValidator.validateWeight("75,5")) // comma decimal separator
        assertNull(ProfileValidator.validateWeight("500.0"))
    }

    // =========================================================================
    // Валидация возраста (validateAge: 14 .. 120 лет)
    // =========================================================================

    @Test
    fun `validateAge returns Empty for empty or blank input`() {
        assertEquals(FieldValidationError.Empty, ProfileValidator.validateAge(""))
        assertEquals(FieldValidationError.Empty, ProfileValidator.validateAge("   "))
    }

    @Test
    fun `validateAge returns NotANumber for non-integer inputs`() {
        assertEquals(FieldValidationError.NotANumber, ProfileValidator.validateAge("abc"))
        assertEquals(FieldValidationError.NotANumber, ProfileValidator.validateAge("25.5"))
    }

    @Test
    fun `validateAge returns NegativeOrZero for zero or negative values`() {
        assertEquals(FieldValidationError.NegativeOrZero, ProfileValidator.validateAge("0"))
        assertEquals(FieldValidationError.NegativeOrZero, ProfileValidator.validateAge("-5"))
    }

    @Test
    fun `validateAge returns OutOfRange for 13 years or 121 years`() {
        assertEquals(FieldValidationError.OutOfRange(min = 14, max = 120), ProfileValidator.validateAge("13"))
        assertEquals(FieldValidationError.OutOfRange(min = 14, max = 120), ProfileValidator.validateAge("10"))
        assertEquals(FieldValidationError.OutOfRange(min = 14, max = 120), ProfileValidator.validateAge("121"))
        assertEquals(FieldValidationError.OutOfRange(min = 14, max = 120), ProfileValidator.validateAge("150"))
    }

    @Test
    fun `validateAge returns null for boundary and valid ages`() {
        assertNull(ProfileValidator.validateAge("14"))
        assertNull(ProfileValidator.validateAge("25"))
        assertNull(ProfileValidator.validateAge("120"))
    }

    // =========================================================================
    // Валидация недельного бюджета (validateWeeklyBudget: 1.0 .. 1_000_000.0)
    // =========================================================================

    @Test
    fun `validateWeeklyBudget returns Empty for empty or blank input`() {
        assertEquals(FieldValidationError.Empty, ProfileValidator.validateWeeklyBudget(""))
        assertEquals(FieldValidationError.Empty, ProfileValidator.validateWeeklyBudget("  "))
    }

    @Test
    fun `validateWeeklyBudget returns NotANumber for non-numeric input`() {
        assertEquals(FieldValidationError.NotANumber, ProfileValidator.validateWeeklyBudget("abc"))
        assertEquals(FieldValidationError.NotANumber, ProfileValidator.validateWeeklyBudget("5000р"))
    }

    @Test
    fun `validateWeeklyBudget returns NegativeOrZero for zero or negative values`() {
        assertEquals(FieldValidationError.NegativeOrZero, ProfileValidator.validateWeeklyBudget("0"))
        assertEquals(FieldValidationError.NegativeOrZero, ProfileValidator.validateWeeklyBudget("0.0"))
        assertEquals(FieldValidationError.NegativeOrZero, ProfileValidator.validateWeeklyBudget("-500"))
    }

    @Test
    fun `validateWeeklyBudget returns OutOfRange for below 1_0 or above 1_000_000_0`() {
        assertEquals(FieldValidationError.OutOfRange(min = 1.0, max = 1_000_000.0), ProfileValidator.validateWeeklyBudget("0.5"))
        assertEquals(FieldValidationError.OutOfRange(min = 1.0, max = 1_000_000.0), ProfileValidator.validateWeeklyBudget("1000000.1"))
        assertEquals(FieldValidationError.OutOfRange(min = 1.0, max = 1_000_000.0), ProfileValidator.validateWeeklyBudget("2000000"))
    }

    @Test
    fun `validateWeeklyBudget returns null for valid budget values`() {
        assertNull(ProfileValidator.validateWeeklyBudget("1.0"))
        assertNull(ProfileValidator.validateWeeklyBudget("5000"))
        assertNull(ProfileValidator.validateWeeklyBudget("7500,50"))
        assertNull(ProfileValidator.validateWeeklyBudget("1000000.0"))
    }

    // =========================================================================
    // Валидация калорий (validateDailyCalories: 500 .. 10_000)
    // =========================================================================

    @Test
    fun `validateDailyCalories returns Empty for blank, NotANumber for letters, NegativeOrZero for non-positive`() {
        assertEquals(FieldValidationError.Empty, ProfileValidator.validateDailyCalories(""))
        assertEquals(FieldValidationError.NotANumber, ProfileValidator.validateDailyCalories("xyz"))
        assertEquals(FieldValidationError.NegativeOrZero, ProfileValidator.validateDailyCalories("0"))
        assertEquals(FieldValidationError.NegativeOrZero, ProfileValidator.validateDailyCalories("-100"))
    }

    @Test
    fun `validateDailyCalories returns OutOfRange for below 500 or above 10000`() {
        assertEquals(FieldValidationError.OutOfRange(min = 500, max = 10000), ProfileValidator.validateDailyCalories("300"))
        assertEquals(FieldValidationError.OutOfRange(min = 500, max = 10000), ProfileValidator.validateDailyCalories("499"))
        assertEquals(FieldValidationError.OutOfRange(min = 500, max = 10000), ProfileValidator.validateDailyCalories("10001"))
        assertEquals(FieldValidationError.OutOfRange(min = 500, max = 10000), ProfileValidator.validateDailyCalories("12000"))
    }

    @Test
    fun `validateDailyCalories returns null for boundary and valid values`() {
        assertNull(ProfileValidator.validateDailyCalories("500"))
        assertNull(ProfileValidator.validateDailyCalories("2200"))
        assertNull(ProfileValidator.validateDailyCalories("10000"))
    }

    // =========================================================================
    // Комплексная валидация онбординга (validateOnboarding)
    // =========================================================================

    @Test
    fun `validateOnboarding returns isValid true with all null errors for valid onboarding data`() {
        val validData = OnboardingData(
            firstName = "Aleksey",
            lastName = "Codintsev",
            heightCm = "180",
            weightKg = "75.0",
            age = "25",
            gender = Gender.MALE,
            activityLevel = ActivityLevel.MODERATE,
            weeklyBudget = "5000",
            goalType = GoalType.LOSE_WEIGHT,
        )

        val result = ProfileValidator.validateOnboarding(validData)

        assertTrue(result.isValid)
        assertNull(result.nameError)
        assertNull(result.heightError)
        assertNull(result.weightError)
        assertNull(result.ageError)
        assertNull(result.budgetError)
    }

    @Test
    fun `validateOnboarding returns isValid false and reports all invalid field errors`() {
        val invalidData = OnboardingData(
            firstName = "A", // too short (< 2)
            lastName = "Codintsev",
            heightCm = "350", // out of range (> 300)
            weightKg = "15.0", // out of range (< 20.0)
            age = "12", // out of range (< 14)
            weeklyBudget = "-100", // negative
        )

        val result = ProfileValidator.validateOnboarding(invalidData)

        assertFalse(result.isValid)
        assertEquals(FieldValidationError.TooShort(minLength = 2), result.nameError)
        assertEquals(FieldValidationError.OutOfRange(min = 50, max = 300), result.heightError)
        assertEquals(FieldValidationError.OutOfRange(min = 20.0, max = 500.0), result.weightError)
        assertEquals(FieldValidationError.OutOfRange(min = 14, max = 120), result.ageError)
        assertEquals(FieldValidationError.NegativeOrZero, result.budgetError)
    }

    @Test
    fun `validateOnboarding with empty fields returns respective Empty errors`() {
        val emptyData = OnboardingData(
            firstName = "",
            heightCm = "",
            weightKg = "",
            age = "",
            weeklyBudget = "",
        )

        val result = ProfileValidator.validateOnboarding(emptyData)

        assertFalse(result.isValid)
        assertEquals(FieldValidationError.Empty, result.nameError)
        assertEquals(FieldValidationError.Empty, result.heightError)
        assertEquals(FieldValidationError.Empty, result.weightError)
        assertEquals(FieldValidationError.Empty, result.ageError)
        assertEquals(FieldValidationError.Empty, result.budgetError)
    }
}
