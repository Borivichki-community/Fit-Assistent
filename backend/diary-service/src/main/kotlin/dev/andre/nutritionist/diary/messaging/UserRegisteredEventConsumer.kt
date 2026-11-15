package dev.andre.nutritionist.diary.messaging

import dev.andre.nutritionist.contracts.avro.UserRegisteredEvent
import dev.andre.nutritionist.diary.domain.model.UserId
import dev.andre.nutritionist.diary.service.DiaryInitializationService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import kotlin.time.Instant

@Component
@ConditionalOnProperty(prefix = "diary.kafka.registration", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class UserRegisteredEventConsumer(
    private val initialization: DiaryInitializationService,
) {
    private val logger = LoggerFactory.getLogger(UserRegisteredEventConsumer::class.java)

    @KafkaListener(
        id = "diary-user-registered",
        topics = ["\${diary.kafka.registration.topic:user.registered}"],
        groupId = "\${spring.kafka.consumer.group-id:diary-service}",
        containerFactory = "userRegisteredKafkaListenerContainerFactory",
        autoStartup = "\${spring.kafka.listener.auto-startup:true}",
    )
    fun consume(
        record: ConsumerRecord<String, UserRegisteredEvent>,
        acknowledgment: Acknowledgment,
    ) {
        val event = requireNotNull(record.value()) { "Registration event is required" }
        val userId = UserId(requireNotNull(event.id) { "User id is required" }.toString())
        val timestamp = requireNotNull(event.registrationTimestamp) { "Registration timestamp is required" }
        require(timestamp.epochSecond in -62_135_596_800L..253_402_300_799L) { "Invalid registration timestamp" }
        MDC.putCloseable("userId", userId.raw).use {
            val initialized =
                initialization.initialize(userId, Instant.fromEpochSeconds(timestamp.epochSecond, timestamp.nano))
            acknowledgment.acknowledge()
            logger.info(
                "Registration event processed: initialized={}, partition={}, offset={}",
                initialized,
                record.partition(),
                record.offset(),
            )
        }
    }
}
