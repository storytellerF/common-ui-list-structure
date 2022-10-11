import common_ui_list_structure_preset.Versions
import common_ui_list_structure_preset.baseLibrary
import common_ui_list_structure_preset.unitTestDependency
import common_ui_list_structure_preset.setupExtFuncSupport

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    defaultConfig {
        minSdk = 21
    }
    namespace = "com.storyteller_f.common_vm_ktx"
}
baseLibrary()
setupExtFuncSupport()
dependencies {
    implementation(project(":ext-func-definition"))
    implementation("androidx.appcompat:appcompat:${Versions.appcompatVersion}")
    unitTestDependency()

    implementation("org.jetbrains.kotlin:kotlin-stdlib:${Versions.kotlinVersion}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")

}