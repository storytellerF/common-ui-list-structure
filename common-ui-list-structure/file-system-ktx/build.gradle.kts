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

    namespace = "com.storyteller_f.file_system_ktx"
}
baseLibrary()
dependencies {
    implementation(project(":file-system"))
    implementation("androidx.core:core-ktx:${Versions.coreVersion}")
    implementation("androidx.appcompat:appcompat:${Versions.appcompatVersion}")
    implementation("com.google.android.material:material:${Versions.materialVersion}")
    implementation("com.j256.simplemagic:simplemagic:1.17")
    unitTestDependency()
}