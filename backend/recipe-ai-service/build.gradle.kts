plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSpring)
    alias(libs.plugins.springBoot)
    alias(libs.plugins.ktlint)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    testImplementation(platform(libs.spring.boot.bom))

    implementation(libs.spring.boot.starter)
    implementation(libs.spring.starter.actuator)
    implementation(libs.kotlin.reflect)

    implementation(libs.spring.starter.grpc)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.spring.starter.redis)

    testImplementation(libs.spring.starter.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.grpc.inprocess)
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.junit)

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
