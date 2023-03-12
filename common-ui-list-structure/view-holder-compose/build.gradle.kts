import com.storyteller_f.version_manager.*

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.storyteller_f.version_manager")

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