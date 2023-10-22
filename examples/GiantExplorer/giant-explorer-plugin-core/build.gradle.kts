import com.storyteller_f.version_manager.*

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.storyteller_f.version_manager")
}
android {
    namespace = "com.storyteller_f.plugin_core"

    defaultConfig {
        minSdk = 21
    }

}
baseLibrary()

dependencies {
    commonAndroidDependency()
    unitTestDependency()
}