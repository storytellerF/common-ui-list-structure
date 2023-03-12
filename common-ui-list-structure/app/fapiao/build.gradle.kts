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
        applicationId = "com.storyteller_f.fapiao"
    }
    namespace = "com.storyteller_f.fapiao"
}

dependencies {
    implementation(fileTree("libs"))
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    networkDependency()
}
baseApp()
setupGeneric()
setupDataBinding()
setupDipToPx()