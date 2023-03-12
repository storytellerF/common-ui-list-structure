import com.storyteller_f.version_manager.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("com.storyteller_f.version_manager")
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