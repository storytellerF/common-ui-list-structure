import com.storyteller_f.version_manager.*

import org.gradle.kotlin.dsl.android
import org.gradle.kotlin.dsl.api
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.implementation
import org.gradle.kotlin.dsl.project

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.storyteller_f.version_manager")
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
    implementation(project(":compat-ktx"))
    implementation(project(":common-vm-ktx"))
    implementation("androidx.navigation:navigation-runtime-ktx:2.6.0")
    api("androidx.core:core-ktx:${Versions.coreVersion}")
    api("androidx.appcompat:appcompat:${Versions.appcompatVersion}")
    api("com.google.android.material:material:${Versions.materialVersion}")
    unitTestDependency()
    implementation("androidx.databinding:viewbinding:${Versions.dataBindingCompilerVersion}")
}