package com.app2.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("com.google.dagger.hilt.android")
                apply("com.google.devtools.ksp")
            }

            extensions.configure<CommonExtension<*, *, *, *, *, *>> {
                compileSdk = 35

                defaultConfig {
                    minSdk = 26
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_21
                    targetCompatibility = JavaVersion.VERSION_21
                }

                buildFeatures {
                    compose = true
                }
            }

            tasks.withType<KotlinCompile>().configureEach {
                kotlinOptions {
                    jvmTarget = "21"
                }
            }

            dependencies {
                add("implementation", project(":core:domain"))
                add("implementation", "androidx.core:core-ktx:1.15.0")
                add("implementation", "androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
                add("implementation", "androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
                add("implementation", "androidx.hilt:hilt-navigation-compose:1.2.0")

                add("implementation", platform("androidx.compose:compose-bom:2024.12.01"))
                add("implementation", "androidx.compose.ui:ui")
                add("implementation", "androidx.compose.ui:ui-graphics")
                add("implementation", "androidx.compose.ui:ui-tooling-preview")
                add("implementation", "androidx.compose.material3:material3")
                add("implementation", "androidx.compose.material:material-icons-extended")
                add("debugImplementation", "androidx.compose.ui:ui-tooling")

                add("implementation", "com.google.dagger:hilt-android:2.52")
                add("ksp", "com.google.dagger:hilt-compiler:2.52")
            }
        }
    }
}
