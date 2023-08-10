import com.storyteller_f.version_manager.baseLibrary
import com.storyteller_f.version_manager.unitTestDependency

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
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
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
    // https://mvnrepository.com/artifact/androidx.test.uiautomator/uiautomator
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0-alpha03")

    debugImplementation("androidx.lifecycle:lifecycle-runtime-ktx:${com.storyteller_f.version_manager.Versions.lifecycleVersion}")
    debugImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${com.storyteller_f.version_manager.Versions.coroutinesVersion}")
    debugImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${com.storyteller_f.version_manager.Versions.coroutinesVersion}")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.google.code.gson:gson:2.10.1")
}