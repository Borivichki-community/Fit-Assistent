import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.time.Duration

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.ktlint)
}

kotlin {
    jvmToolchain(21)
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
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
            }
        }
    }
}

sourceSets {
    main {
        proto {
            srcDir("../../proto")
            include("nutritionist/diary/v1/**", "nutritionist/profile/v1/**")
        }
    }
}

dependencies {
    implementation(platform(libs.spring.boot.bom))
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    compileOnly(libs.javax.annotation.api)

    testImplementation(platform(libs.spring.boot.bom))
    testImplementation(libs.spring.starter.test)
    testImplementation(libs.testcontainers.core)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.grpc.netty)
    testImplementation(libs.grpc.services)
    testImplementation(libs.spring.kafka)
    testImplementation(libs.confluent.avro.serializer) {
        exclude(group = "org.apache.kafka", module = "kafka-clients")
    }
    testImplementation(project(":shared:contracts"))
    testImplementation(libs.postgresql.jdbc)
    testRuntimeOnly(libs.junit.platform.launcher)
}

evaluationDependsOn(":backend:diary-service")
evaluationDependsOn(":backend:profile-service")

val diaryJar = project(":backend:diary-service").tasks.named<BootJar>("bootJar")
val profileJar = project(":backend:profile-service").tasks.named<BootJar>("bootJar")

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    dependsOn(diaryJar, profileJar)
    inputs.files(diaryJar.flatMap { it.archiveFile }, profileJar.flatMap { it.archiveFile })
    systemProperty(
        "integration.diary.jar",
        diaryJar
            .get()
            .archiveFile
            .get()
            .asFile
            .absolutePath,
    )
    systemProperty(
        "integration.profile.jar",
        profileJar
            .get()
            .archiveFile
            .get()
            .asFile
            .absolutePath,
    )
    systemProperty(
        "integration.logs",
        layout.buildDirectory
            .dir("service-logs")
            .get()
            .asFile
            .absolutePath,
    )
    maxParallelForks = 1
    timeout.set(Duration.ofMinutes(5))
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
    filter {
        exclude { element ->
            element
                .file
                .toPath()
                .startsWith(
                    layout
                        .buildDirectory
                        .get()
                        .asFile
                        .toPath(),
                )
        }
    }
}

tasks.named("check") {
    dependsOn("ktlintCheck")
}
