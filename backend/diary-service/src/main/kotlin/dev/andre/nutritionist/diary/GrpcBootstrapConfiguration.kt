package dev.andre.nutritionist.diary

import io.grpc.BindableService
import io.grpc.protobuf.services.HealthStatusManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class GrpcBootstrapConfiguration {
    @Bean
    fun bootstrapGrpcHealthService(healthStatusManager: HealthStatusManager): BindableService = healthStatusManager.healthService
}
