package com.app2.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.google.devtools.ksp")
            }

            dependencies {
                add("implementation", "androidx.room:room-runtime:2.6.1")
                add("implementation", "androidx.room:room-ktx:2.6.1")
                add("ksp", "androidx.room:room-compiler:2.6.1")
            }
        }
    }
}
