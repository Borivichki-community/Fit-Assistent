import com.google.protobuf.gradle.proto

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.protobuf)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(libs.grpc.stub)
    api(libs.grpc.protobuf)
    api(libs.protobuf.kotlin)
    api(libs.grpc.kotlin.stub)
    api(libs.kotlinx.coroutines.core)
}

sourceSets {
    main {
        proto {
            srcDir("../../proto")
        }
    }
}

protobuf {
    protoc {
        artifact = libs.codegen.protoc.get().toString()
    }
    plugins {
        create("grpc") {
            artifact = libs.codegen.grpc.java.get().toString()
        }
        create("grpckt") {
            artifact = "${libs.codegen.grpc.kotlin.get()}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                maybeCreate("grpc")
                maybeCreate("grpckt")
            }
            task.builtins {
                maybeCreate("kotlin")
            }
        }
    }
}
