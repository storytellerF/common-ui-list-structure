import common_ui_list_structure_preset.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android")
    id("kotlin-parcelize")
}

android {
    defaultConfig {
        applicationId = "com.storyteller_f.fapiao"
    }
    namespace = "com.storyteller_f.fapiao"
}

dependencies {
    implementation(fileTree("libs"))
    implementation(project(":fapiao-reader"))
    implementation("com.tom-roush:pdfbox-android:2.0.26.0")

    networkDependency()
}
baseApp()
setupGeneric()
setupDataBinding()
setupDipToPx()