import com.google.protobuf.gradle.proto
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.protobuf)
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
        ofBuildType("debug").forEach { task ->
            task.builtins {
                maybeCreate("java")
            }
            task.plugins {
                create("grpc") {
                    option("@generated=omit")
                }
                create("grpckt")
            }
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":mobile:shared"))

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.google.material)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.kotlinx.datetime)
    implementation(libs.appauth)
    implementation(libs.androidx.security.crypto)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
    debugImplementation(libs.grpc.okhttp)
    debugImplementation(libs.grpc.protobuf)
    debugImplementation(libs.grpc.stub)
    debugImplementation(libs.grpc.kotlin.stub)
    debugImplementation(libs.protobuf.java)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.compose.test.junit4)
    debugImplementation(libs.compose.test.manifest)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.testExt.junit)
    androidTestImplementation(libs.compose.test.junit4)
}

android {
    namespace = "dev.andre.nutritionist.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "dev.andre.nutritionist.android"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appAuthRedirectScheme"] = "com.budgetntr.fitassistant"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    sourceSets {
        getByName("debug").proto {
            srcDir("../../proto")
            setIncludes(
                listOf(
                    "nutritionist/diary/v1/diary.proto",
                    "nutritionist/catalog/v1/catalog.proto",
                    "nutritionist/recipe/v1/recipe.proto",
                    "health.proto",
                ),
            )
        }
    }
    buildFeatures {
        compose = true
    }
}
