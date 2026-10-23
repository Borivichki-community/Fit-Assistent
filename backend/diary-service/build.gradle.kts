plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSpring)
    alias(libs.plugins.kotlinJpa)
    alias(libs.plugins.springBoot)
    alias(libs.plugins.ktlint)
}

kotlin {
    jvmToolchain(21)
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    testImplementation(platform(libs.spring.boot.bom))

    implementation(libs.spring.boot.starter)
    implementation(libs.spring.starter.actuator)
    implementation(libs.kotlin.reflect)

    implementation(libs.spring.starter.grpc)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.spring.starter.jpa)
    implementation(libs.spring.starter.liquibase)
    runtimeOnly(libs.postgresql.jdbc)

    implementation(libs.spring.kafka)
    implementation(libs.avro.runtime)
    implementation(libs.confluent.avro.serializer) {
        // Исключаем Confluent-сборку клиента с другой шкалой версий (*-ccs).
        // Сам Kafka client остаётся через Spring Kafka; его версия задаётся Boot BOM.
        exclude(group = "org.apache.kafka", module = "kafka-clients")
    }

    testImplementation(libs.spring.starter.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.spring.kafka.test)
    testImplementation(libs.grpc.inprocess)
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)

    testRuntimeOnly(libs.junit.platform.launcher)
}

val ktlintEngineVersion =
    extensions
        .getByType<VersionCatalogsExtension>()
        .named("libs")
        .findVersion("ktlint")
        .get()
        .requiredVersion

ktlint {
    version.set(ktlintEngineVersion)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named("check") {
    dependsOn("ktlintCheck")
}
