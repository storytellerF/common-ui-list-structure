import common_ui_list_structure_preset.Versions
import common_ui_list_structure_preset.baseLibrary
import common_ui_list_structure_preset.unitTestDependency
plugins {
    id ("com.android.library")
    id ("org.jetbrains.kotlin.android")
    id ("kotlin-parcelize")
}

android {

    defaultConfig {
        minSdk = 21
    }
    namespace = "com.storyteller_f.fapiao_reader"
}
baseLibrary()
dependencies {
    implementation ("androidx.core:core-ktx:${Versions.coreVersion}")
    implementation ("androidx.appcompat:appcompat:${Versions.appcompatVersion}")
    implementation ("com.google.android.material:material:${Versions.materialVersion}")
    unitTestDependency()
    implementation ("com.tom-roush:pdfbox-android:2.0.25.0")
}