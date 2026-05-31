---
name: android-build-ci
description: Android build system and CI/CD — Gradle Kotlin DSL, version catalogs, convention plugins, code quality tools, and GitHub Actions workflows for automated build, test, and deploy.
---

# Android Build & CI/CD

## When to Use This Skill
- Setting up Gradle build scripts with Kotlin DSL
- Configuring version catalogs for dependency management
- Creating convention plugins for multi-module projects
- Setting up code quality (Detekt, Lint, Spotless)
- Configuring CI/CD with GitHub Actions for Android

## Version Catalog (libs.versions.toml)

```toml
[versions]
agp = "8.5.2"
kotlin = "2.0.21"
compose-bom = "2024.12.01"
hilt = "2.52"
room = "2.6.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version = "1.15.0" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version = "2.8.7" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ksp = { id = "com.google.devtools.ksp", version = "2.0.21-1.0.27" }
```

## Convention Plugin Example

```kotlin
// build-logic/convention/src/main/kotlin/AndroidFeatureConventionPlugin.kt
class AndroidFeatureConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    with(target) {
      plugins.apply("com.android.library")
      plugins.apply("org.jetbrains.kotlin.android")
      plugins.apply("org.jetbrains.kotlin.plugin.compose")

      android { namespace = "${namespace}.${moduleName}" }

      dependencies {
        add("implementation", project(":core:domain"))
        add("implementation", project(":core:ui"))
        add("implementation", libs.findLibrary("hilt-android").get())
        add("ksp", libs.findLibrary("hilt-compiler").get())
      }
    }
  }
}
```

## Code Quality

### Detekt Configuration
```yaml
# config/detekt.yml
build:
  maxIssues: 30
style:
  MagicNumber:
    active: false
  UnusedParameter:
    active: false
compose:
  ComposableNaming:
    active: true
  ModifierMissing:
    active: true
  ModifierReused:
    active: true
```

### Detekt + Lint Commands
```bash
./gradlew detekt                # Static analysis (Kotlin)
./gradlew lint                  # Android Lint (XML + resources)
./gradlew spotlessApply         # Auto-format code
./gradlew :app:dependencyAnalysis  # Dependency insights
```

## GitHub Actions CI

```yaml
# .github/workflows/ci.yml
name: CI
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '21' }
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew assembleDebug testDebugUnitTest detekt lint
  release:
    if: startsWith(github.ref, 'refs/tags/')
    steps:
      - run: ./gradlew assembleRelease bundleRelease
      - uses: actions/upload-artifact@v4
        with:
          name: release-aab
          path: app/build/outputs/bundle/release/app-release.aab
```

## Gradle Performance Tips

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
android.native.buildOutput=verbose
```

## build.gradle.kts (App Module)
```kotlin
plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.hilt)
  alias(libs.plugins.ksp)
}

android {
  namespace = "com.example.app"
  compileSdk = 35
  defaultConfig {
    minSdk = 26
    targetSdk = 35
  }
  buildFeatures { compose = true }
}
```
