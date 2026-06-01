pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "App2"

includeBuild("build-logic")

include(":app")
include(":core:data")
include(":core:domain")
include(":core:ui")
include(":feature:dashboard")
include(":feature:products")
include(":feature:pos")
include(":feature:customers")
include(":feature:suppliers")
include(":feature:warehouses")
include(":feature:orders")
include(":feature:invoices")
include(":feature:notifications")
    include(":feature:auth")
    include(":feature:settings")
