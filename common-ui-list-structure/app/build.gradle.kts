import common_ui_list_structure_preset.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android")
    id("kotlin-parcelize")
}

android {

    defaultConfig {
        applicationId = "com.storyteller_f.common_ui_list_structure"
    }

    namespace = "com.storyteller_f.common_ui_list_structure"
}

dependencies {
    implementation(fileTree("libs"))

    networkDependency()
}
baseApp()
setupGeneric()
setupDataBinding()
setupDipToPx()