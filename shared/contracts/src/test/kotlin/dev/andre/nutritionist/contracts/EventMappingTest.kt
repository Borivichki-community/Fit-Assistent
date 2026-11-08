package dev.andre.nutritionist.contracts

import dev.andre.nutritionist.contracts.avro.CaloriesConsumedEvent
import dev.andre.nutritionist.contracts.avro.UserRegisteredEvent
import dev.andre.nutritionist.contracts.domain.Meal
import dev.andre.nutritionist.contracts.domain.User
import dev.andre.nutritionist.contracts.mapper.CaloriesConsumedEventMapper
import dev.andre.nutritionist.contracts.mapper.UserRegisteredEventMapper
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import org.apache.avro.Schema
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.specific.SpecificDatumWriter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Тесты маппинга контрактов Avro в доменные модели и верификации BACKWARD-совместимости.
 * Согласно AGENTS.md §5.1, запрещено использование MockK — только реальные объекты и чистые проверки.
 */
class EventMappingTest {
    @Test
    fun `mapping UserRegisteredEvent to domain User and back preserves all typed fields`() {
        val uuid = UUID.randomUUID()
        val timestamp =
            java.time.Instant
                .now()
                .truncatedTo(ChronoUnit.MILLIS)
        val avroEvent =
            UserRegisteredEvent
                .newBuilder()
                .setId(uuid)
                .setEmail("nutritionist.user@example.com")
                .setRegistrationTimestamp(timestamp)
                .setSource("google_oauth2")
                .build()

        val domainUser: User = UserRegisteredEventMapper.toDomain(avroEvent)

        assertEquals(uuid.toString(), domainUser.id.raw)
        assertEquals("nutritionist.user@example.com", domainUser.email)
        assertEquals(timestamp.toEpochMilli(), domainUser.registeredAt.toEpochMilliseconds())
        assertEquals("google_oauth2", domainUser.source)

        val mappedBackAvro = UserRegisteredEventMapper.toAvro(domainUser)
        assertEquals(avroEvent.id, mappedBackAvro.id)
        assertEquals(avroEvent.email, mappedBackAvro.email)
        assertEquals(avroEvent.registrationTimestamp, mappedBackAvro.registrationTimestamp)
        assertEquals(avroEvent.source, mappedBackAvro.source)
    }

    @Test
    fun `mapping CaloriesConsumedEvent to domain Meal and back preserves typed IDs and calories`() {
        val userUuid = UUID.randomUUID()
        val mealUuid = UUID.randomUUID()
        val timestamp =
            java.time.Instant
                .now()
                .truncatedTo(ChronoUnit.MILLIS)

        val avroEvent =
            CaloriesConsumedEvent
                .newBuilder()
                .setUserId(userUuid)
                .setMealId(mealUuid)
                .setCalories(550)
                .setTimestamp(timestamp)
                .setMealType("LUNCH")
                .setNotes("Steamed chicken with buckwheat")
                .build()

        val domainMeal: Meal = CaloriesConsumedEventMapper.toDomain(avroEvent)

        assertEquals(userUuid.toString(), domainMeal.userId.raw)
        assertEquals(mealUuid.toString(), domainMeal.mealId.raw)
        assertEquals(550, domainMeal.calories)
        assertEquals(timestamp.toEpochMilli(), domainMeal.timestamp.toEpochMilliseconds())
        assertEquals("LUNCH", domainMeal.mealType)
        assertEquals("Steamed chicken with buckwheat", domainMeal.notes)

        val mappedBackAvro = CaloriesConsumedEventMapper.toAvro(domainMeal)
        assertEquals(avroEvent.userId, mappedBackAvro.userId)
        assertEquals(avroEvent.mealId, mappedBackAvro.mealId)
        assertEquals(avroEvent.calories, mappedBackAvro.calories)
        assertEquals(avroEvent.timestamp, mappedBackAvro.timestamp)
        assertEquals(avroEvent.mealType, mappedBackAvro.mealType)
        assertEquals(avroEvent.notes, mappedBackAvro.notes)
    }

    @Test
    fun `mapping CaloriesConsumedEvent with absent optional fields respects schema defaults`() {
        val userUuid = UUID.randomUUID()
        val mealUuid = UUID.randomUUID()
        val timestamp =
            java.time.Instant
                .now()
                .truncatedTo(ChronoUnit.MILLIS)

        // Создаем событие БЕЗ установки опциональных полей (mealType, notes)
        val avroEvent =
            CaloriesConsumedEvent
                .newBuilder()
                .setUserId(userUuid)
                .setMealId(mealUuid)
                .setCalories(250)
                .setTimestamp(timestamp)
                .build()

        // Проверяем дефолтные значения схемы Avro
        assertNull(avroEvent.mealType, "Avro schema default for mealType must be null")
        assertNull(avroEvent.notes, "Avro schema default for notes must be null")

        val domainMeal = CaloriesConsumedEventMapper.toDomain(avroEvent)

        assertEquals(userUuid.toString(), domainMeal.userId.raw)
        assertEquals(mealUuid.toString(), domainMeal.mealId.raw)
        assertEquals(250, domainMeal.calories)
        assertNull(domainMeal.mealType)
        assertNull(domainMeal.notes)
    }

    @Test
    fun `mapping UserRegisteredEvent with absent optional source respects schema default`() {
        val uuid = UUID.randomUUID()
        val timestamp =
            java.time.Instant
                .now()
                .truncatedTo(ChronoUnit.MILLIS)

        val avroEvent =
            UserRegisteredEvent
                .newBuilder()
                .setId(uuid)
                .setEmail("minimal@domain.org")
                .setRegistrationTimestamp(timestamp)
                .build()

        assertNull(avroEvent.source, "Avro schema default for source must be null")

        val domainUser = UserRegisteredEventMapper.toDomain(avroEvent)
        assertEquals(uuid.toString(), domainUser.id.raw)
        assertEquals("minimal@domain.org", domainUser.email)
        assertNull(domainUser.source)
    }

    @Test
    fun `binary roundtrip with SpecificDatumWriter and SpecificDatumReader verifies record serialization`() {
        val original =
            UserRegisteredEvent
                .newBuilder()
                .setId(UUID.randomUUID())
                .setEmail("binary.serialization@fitassistant.app")
                .setRegistrationTimestamp(
                    java.time.Instant
                        .now()
                        .truncatedTo(ChronoUnit.MILLIS),
                ).setSource("direct_sign_up")
                .build()

        val out = ByteArrayOutputStream()
        val encoder = EncoderFactory.get().binaryEncoder(out, null)
        val writer = SpecificDatumWriter<UserRegisteredEvent>(UserRegisteredEvent.getClassSchema())
        writer.write(original, encoder)
        encoder.flush()

        val bytes = out.toByteArray()
        assertTrue(bytes.isNotEmpty())

        val decoder = DecoderFactory.get().binaryDecoder(bytes, null)
        val reader = SpecificDatumReader<UserRegisteredEvent>(UserRegisteredEvent.getClassSchema())
        val decoded = reader.read(null, decoder)

        assertEquals(original.id, decoded.id)
        assertEquals(original.email, decoded.email)
        assertEquals(original.registrationTimestamp, decoded.registrationTimestamp)
        assertEquals(original.source, decoded.source)
    }

    @Test
    fun `schema registry client verifies BACKWARD evolution compatibility and rejects breaking changes`() {
        val registry = MockSchemaRegistryClient()
        val subject = "user.registered-value"

        val schemaV1 = UserRegisteredEvent.getClassSchema()
        registry.register(subject, schemaV1)

        // 1. Схема совместима сама с собой в режиме BACKWARD
        assertTrue(registry.testCompatibility(subject, schemaV1), "Schema must be compatible with itself")

        // 2. Добавление поля С ДЕФОЛТОМ совместимо (BACKWARD compliant evolution)
        val compatibleSchemaV2 =
            Schema.Parser().parse(
                """
                {
                  "type": "record",
                  "name": "UserRegisteredEvent",
                  "namespace": "dev.andre.nutritionist.contracts.avro",
                  "fields": [
                    { "name": "id", "type": { "type": "string", "logicalType": "uuid" } },
                    { "name": "email", "type": "string" },
                    { "name": "registrationTimestamp", "type": { "type": "long", "logicalType": "timestamp-millis" } },
                    { "name": "source", "type": ["null", "string"], "default": null },
                    { "name": "locale", "type": ["null", "string"], "default": null }
                  ]
                }
                """.trimIndent(),
            )
        val isV2Compatible = registry.testCompatibility(subject, compatibleSchemaV2)
        assertTrue(isV2Compatible, "Adding a field with a default value must be BACKWARD compatible")

        // 3. Добавление обязательного поля БЕЗ ДЕФОЛТА ломает совместимость (BREAKING change)
        val breakingSchemaV3 =
            Schema.Parser().parse(
                """
                {
                  "type": "record",
                  "name": "UserRegisteredEvent",
                  "namespace": "dev.andre.nutritionist.contracts.avro",
                  "fields": [
                    { "name": "id", "type": { "type": "string", "logicalType": "uuid" } },
                    { "name": "email", "type": "string" },
                    { "name": "registrationTimestamp", "type": { "type": "long", "logicalType": "timestamp-millis" } },
                    { "name": "source", "type": ["null", "string"], "default": null },
                    { "name": "mandatoryPhoneNumber", "type": "string" }
                  ]
                }
                """.trimIndent(),
            )
        val isV3Compatible = registry.testCompatibility(subject, breakingSchemaV3)
        assertFalse(isV3Compatible, "Adding a required field without default must be rejected in BACKWARD mode")
    }
}
