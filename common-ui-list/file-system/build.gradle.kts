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

    namespace = "com.storyteller_f.file_system"
}
baseLibrary()
dependencies {
    implementation(project(":common-ktx"))

    implementation(project(":multi-core"))
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    unitTestDependency()
    implementation(project(":compat-ktx"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
}