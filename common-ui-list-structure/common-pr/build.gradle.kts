import common_ui_list_structure_preset.Versions
import common_ui_list_structure_preset.baseLibrary
import common_ui_list_structure_preset.setupExtFuncSupport
import common_ui_list_structure_preset.unitTestDependency

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    defaultConfig {
        minSdk = 21
    }
    kotlinOptions {
        freeCompilerArgs = listOf("-Xcontext-receivers")
    }
    namespace = "com.storyteller_f.common_pr"
}
baseLibrary()
setupExtFuncSupport()
dependencies {
    implementation(project(":ext-func-definition"))
    implementation(project(":common-ktx"))
    implementation("androidx.core:core-ktx:${Versions.coreVersion}")
    implementation("androidx.appcompat:appcompat:${Versions.appcompatVersion}")
    implementation("com.google.android.material:material:${Versions.materialVersion}")
    unitTestDependency()
}
