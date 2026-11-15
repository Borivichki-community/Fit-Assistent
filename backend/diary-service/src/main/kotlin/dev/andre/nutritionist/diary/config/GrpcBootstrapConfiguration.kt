package dev.andre.nutritionist.diary.config

import io.grpc.BindableService
import io.grpc.protobuf.services.HealthStatusManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration(proxyBeanMethods = false)
@EnableScheduling
class GrpcBootstrapConfiguration {
    @Bean
    fun bootstrapGrpcHealthService(healthStatusManager: HealthStatusManager): BindableService = healthStatusManager.healthService
}
