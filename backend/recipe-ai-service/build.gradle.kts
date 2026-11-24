plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSpring)
    alias(libs.plugins.kotlinJpa)
    alias(libs.plugins.springBoot)
    alias(libs.plugins.protobuf)
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

protobuf {
    protoc {
        artifact =
            libs.codegen.protoc
                .get()
                .toString()
    }
    plugins {
        create("grpc") {
            artifact =
                libs.codegen.grpc.java
                    .get()
                    .toString()
        }
        create("grpckt") {
            artifact = "${libs.codegen.grpc.kotlin.get()}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                maybeCreate("grpc")
                create("grpckt")
            }
        }
    }
}

sourceSets {
    main {
        proto {
            srcDir("../../proto")
            include(
                "nutritionist/recipe/v1/*.proto",
                "nutritionist/catalog/v1/*.proto",
                "nutritionist/diary/v1/*.proto",
                "nutritionist/profile/v1/*.proto",
            )
        }
    }
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
    implementation(libs.postgresql.jdbc)
    implementation(libs.spring.starter.liquibase)
    implementation(libs.spring.starter.redis)
    implementation(libs.spring.starter.web)
    implementation(libs.spring.starter.jackson)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.kotlinx.coroutines.slf4j)
    implementation(libs.micrometer.prometheus)
    implementation(libs.resilience4j.circuitbreaker)
    implementation(libs.resilience4j.retry)
    implementation(libs.resilience4j.timelimiter)
    implementation(libs.resilience4j.kotlin)
    implementation(libs.resilience4j.micrometer)

    testImplementation(libs.wiremock.standalone)

    testImplementation(libs.spring.starter.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.grpc.inprocess)
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)

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
    val generatedDir =
        layout.buildDirectory
            .dir("generated")
            .get()
            .asFile
            .toPath()
    filter {
        exclude { it.file.toPath().startsWith(generatedDir) }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named("check") {
    dependsOn("ktlintCheck")
}
