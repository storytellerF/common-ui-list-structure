import com.storyteller_f.version_manager.*

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("com.storyteller_f.version_manager")
}

android {
    defaultConfig {
        minSdk = 21
    }
    namespace = "com.storyteller_f.common_vm_ktx"
}
baseLibrary()
setupExtFunc()
dependencies {
    implementation(project(":ext-func-definition"))
    implementation("androidx.appcompat:appcompat:${Versions.appcompatVersion}")
    unitTestDependency()

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")
}