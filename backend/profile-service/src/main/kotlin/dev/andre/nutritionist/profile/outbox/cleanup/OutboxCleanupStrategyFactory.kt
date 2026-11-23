package dev.andre.nutritionist.profile.outbox.cleanup

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Фабрика выбора стратегии очистки Outbox-сообщений на основе конфигурации `outbox.cleanup.strategy`.
 */
@Component
class OutboxCleanupStrategyFactory(
    strategies: List<OutboxCleanupStrategy>,
    @Value("\${outbox.cleanup.strategy:BATCH}") private val defaultStrategyName: String,
) {
    private val logger = LoggerFactory.getLogger(OutboxCleanupStrategyFactory::class.java)
    private val strategyMap = strategies.associateBy { it.type }

    fun getStrategy(type: CleanupStrategyType = resolveConfiguredType()): OutboxCleanupStrategy {
        val strategy = strategyMap[type] ?: strategyMap[CleanupStrategyType.BATCH]
        if (strategy == null) {
            logger.error("No cleanup strategy found for type={}, available: {}", type, strategyMap.keys)
            error("No OutboxCleanupStrategy registered for $type")
        }
        return strategy
    }

    private fun resolveConfiguredType(): CleanupStrategyType =
        runCatching { CleanupStrategyType.valueOf(defaultStrategyName.trim().uppercase()) }
            .getOrElse {
                logger.warn("Unknown outbox cleanup strategy '{}', defaulting to BATCH", defaultStrategyName)
                CleanupStrategyType.BATCH
            }
}
