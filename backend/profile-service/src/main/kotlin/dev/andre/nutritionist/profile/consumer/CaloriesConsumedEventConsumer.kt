package dev.andre.nutritionist.profile.consumer

import dev.andre.nutritionist.profile.service.CaloriesConsumptionService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.kafka.annotation.KafkaListener

class CaloriesConsumedEventConsumer(
    private val decoder: CaloriesConsumedEventDecoder,
    private val service: CaloriesConsumptionService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["\${profile.calories.topic}"],
        containerFactory = "profileCaloriesListenerFactory",
    )
    fun consume(record: ConsumerRecord<ByteArray, ByteArray>) {
        val meal = decoder.decode(record)
        val previous = MDC.getCopyOfContextMap()
        try {
            MDC.put("userId", meal.userId.raw)
            record.headers().lastHeader("traceparent")?.value()?.let {
                MDC.put("traceparent", it.toString(Charsets.UTF_8))
            }
            service.consume(meal)
            logger.info(
                "Calorie event processed: mealId={}, partition={}, offset={}",
                meal.mealId.raw,
                record.partition(),
                record.offset(),
            )
        } finally {
            if (previous == null) MDC.clear() else MDC.setContextMap(previous)
        }
    }
}
