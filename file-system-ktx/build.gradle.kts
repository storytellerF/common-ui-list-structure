import common_ui_list_structure_preset.Versions
import common_ui_list_structure_preset.baseLibrary
import common_ui_list_structure_preset.unitTestDependency

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    defaultConfig {
        minSdk = 21
    }

    namespace = "com.storyteller_f.file_system_ktx"
}
baseLibrary()
dependencies {
    implementation(project(":file-system"))
    implementation("androidx.core:core-ktx:${Versions.coreVersion}")
    implementation("androidx.appcompat:appcompat:${Versions.appcompatVersion}")
    implementation("com.google.android.material:material:${Versions.materialVersion}")
    unitTestDependency()
}