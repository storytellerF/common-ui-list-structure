import common_ui_list_structure_preset.Versions
import common_ui_list_structure_preset.baseLibrary
import common_ui_list_structure_preset.unitTestDependency

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {

    defaultConfig {
        minSdk = 16
    }

    namespace = "com.storyteller_f.file_system"
}
baseLibrary()
dependencies {
    implementation(project(":common-ktx"))

    implementation(project(":multi-core"))
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("com.google.android.material:material:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    unitTestDependency()

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    val libsuVersion = "5.0.3"
    implementation("com.github.topjohnwu.libsu:nio:${libsuVersion}")
}