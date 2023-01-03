import common_ui_list_structure_preset.Versions
import common_ui_list_structure_preset.baseLibrary
import common_ui_list_structure_preset.setupCompose
import common_ui_list_structure_preset.setupExtFuncSupport
import common_ui_list_structure_preset.unitTestDependency
import org.gradle.kotlin.dsl.android
import org.gradle.kotlin.dsl.api
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.implementation
import org.gradle.kotlin.dsl.project

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

android {
    defaultConfig {
        minSdk = 21
    }

    buildFeatures {
        dataBinding = true
    }

    namespace = "com.storyteller_f.common_ui"
}
baseLibrary()
setupCompose(true)
setupExtFuncSupport()
dependencies {
    api(project(":ext-func-definition"))
    implementation(project(":common-ktx"))
    api("androidx.core:core-ktx:${Versions.coreVersion}")
    api("androidx.appcompat:appcompat:${Versions.appcompatVersion}")
    api("com.google.android.material:material:${Versions.materialVersion}")
    unitTestDependency()
    implementation("androidx.databinding:viewbinding:${Versions.dataBindingCompilerVersion}")
}