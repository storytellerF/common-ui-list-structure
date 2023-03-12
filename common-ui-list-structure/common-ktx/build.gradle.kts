import com.storyteller_f.version_manager.*

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.storyteller_f.version_manager")
}

android {

    defaultConfig {
        minSdk = 16
    }
    namespace = "com.storyteller_f.common_ktx"
}
baseLibrary()

dependencies {
    implementation("androidx.core:core-ktx:${Versions.coreVersion}")
    implementation("androidx.appcompat:appcompat:${Versions.appcompatVersion}")
    implementation("com.google.android.material:material:${Versions.materialVersion}")
    unitTestDependency()
}