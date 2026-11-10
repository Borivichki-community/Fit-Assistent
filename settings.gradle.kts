rootProject.name = "FitAssistent"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven {
            name = "Confluent"
            url = uri("https://packages.confluent.io/maven/")
        }
    }
}

val backendOnly = providers.gradleProperty("backendOnly").orNull == "true"

if (!backendOnly) {
    include(":mobile:androidApp")
    include(":mobile:shared")
}

include(":shared:contracts")

include(":backend:profile-service")
include(":backend:catalog-price-service")
include(":backend:diary-service")
include(":backend:recipe-ai-service")

//не подключенный в разработку модуль food cv
// include(":backend:food-cv-service")