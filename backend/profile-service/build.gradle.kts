plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSpring)
    alias(libs.plugins.kotlinJpa)
    alias(libs.plugins.springBoot)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
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
                create("grpckt")
            }
            task.builtins {
                create("kotlin")
            }
        }
    }
}

sourceSets {
    main {
        proto {
            srcDir("../../proto")
        }
    }
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    testImplementation(platform(libs.spring.boot.bom))

    implementation(libs.spring.boot.starter)
    implementation(libs.spring.starter.web)
    implementation(libs.spring.starter.actuator)
    implementation(libs.kotlin.reflect)

    implementation(libs.spring.starter.grpc)
    implementation(libs.grpc.protobuf)
    implementation(libs.protobuf.kotlin)
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.slf4j)

    implementation(libs.spring.starter.jpa)
    implementation(libs.spring.starter.liquibase)
    implementation(libs.uuid.creator)
    runtimeOnly(libs.postgresql.jdbc)

    implementation(libs.spring.kafka)
    implementation(libs.avro.runtime)
    implementation(libs.confluent.avro.serializer) {

        exclude(group = "org.apache.kafka", module = "kafka-clients")
    }

    testImplementation(libs.spring.starter.test)
    testImplementation(libs.kotlin.test)
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

tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask>().configureEach {
    exclude { element ->
        element
            .file
            .toPath()
            .startsWith(
                layout
                    .buildDirectory
                    .asFile
                    .get()
                    .toPath(),
            )
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

detekt {
    source.setFrom("src/main/kotlin")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    setSource(files("src/main/kotlin"))
}

tasks.named("check") {
    dependsOn("ktlintCheck")
}
