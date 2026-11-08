package dev.andre.nutritionist.contracts

import dev.andre.nutritionist.contracts.avro.CaloriesConsumedEvent
import dev.andre.nutritionist.contracts.avro.UserRegisteredEvent
import dev.andre.nutritionist.contracts.domain.Meal
import dev.andre.nutritionist.contracts.domain.MealId
import dev.andre.nutritionist.contracts.domain.User
import dev.andre.nutritionist.contracts.domain.UserId
import dev.andre.nutritionist.contracts.mapper.CaloriesConsumedEventMapper
import dev.andre.nutritionist.contracts.mapper.UserRegisteredEventMapper
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class EventMapperEdgeCaseTest {
    @Test
    fun `DomainModels throw IllegalArgumentException on blank UserId or MealId`() {
        assertFailsWith<IllegalArgumentException> {
            UserId("")
        }
        assertFailsWith<IllegalArgumentException> {
            UserId("   ")
        }
        assertFailsWith<IllegalArgumentException> {
            MealId("")
        }
        assertFailsWith<IllegalArgumentException> {
            MealId("   ")
        }
    }

    @Test
    fun `Mapper fallbacks to nameUUIDFromBytes when domain IDs are not UUID strings`() {
        val nonUuidString = "some-legacy-id-12345"

        val user =
            User(
                id = UserId(nonUuidString),
                email = "test@example.com",
                registeredAt = Instant.fromEpochMilliseconds(1000L),
            )

        val avroEvent = UserRegisteredEventMapper.toAvro(user)

        val meal =
            Meal(
                userId = UserId(nonUuidString),
                mealId = MealId("another-legacy-id"),
                calories = 500,
                timestamp = Instant.fromEpochMilliseconds(2000L),
            )

        val mealAvroEvent = CaloriesConsumedEventMapper.toAvro(meal)
        assertEquals(avroEvent.id.toString(), mealAvroEvent.userId.toString())
    }

    @Test
    fun `Mapper throws IllegalArgumentException on missing required fields in Avro payload`() {
        val invalidEvent = UserRegisteredEvent()
        assertFailsWith<IllegalArgumentException> {
            UserRegisteredEventMapper.toDomain(invalidEvent)
        }

        val invalidMealEvent = CaloriesConsumedEvent()
        assertFailsWith<IllegalArgumentException> {
            CaloriesConsumedEventMapper.toDomain(invalidMealEvent)
        }
    }

    @Test
    fun `Mapper handles extreme timestamps safely`() {
        val epochZero = Instant.fromEpochMilliseconds(0L)
        val user0 =
            User(
                id = UserId(Uuid.random().toString()),
                email = "zero@example.com",
                registeredAt = epochZero,
            )
        val avro0 = UserRegisteredEventMapper.toAvro(user0)
        assertEquals(0L, avro0.registrationTimestamp.toEpochMilli())
        val mappedUser0 = UserRegisteredEventMapper.toDomain(avro0)
        assertEquals(epochZero, mappedUser0.registeredAt)

        val negativeEpoch = Instant.fromEpochMilliseconds(-10000000L)
        val userNeg =
            User(
                id = UserId(Uuid.random().toString()),
                email = "negative@example.com",
                registeredAt = negativeEpoch,
            )
        val avroNeg = UserRegisteredEventMapper.toAvro(userNeg)
        assertEquals(-10000000L, avroNeg.registrationTimestamp.toEpochMilli())
        val mappedUserNeg = UserRegisteredEventMapper.toDomain(avroNeg)
        assertEquals(negativeEpoch, mappedUserNeg.registeredAt)
    }
}
