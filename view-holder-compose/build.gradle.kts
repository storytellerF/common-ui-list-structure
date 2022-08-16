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

    namespace = "com.storyteller_f.view_holder_compose"
}
baseLibrary()

dependencies {

    unitTestDependency()
    api("androidx.compose.ui:ui:${Versions.composeVersion}")

    implementation(project(":ui-list"))
}