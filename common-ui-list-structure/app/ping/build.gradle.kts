import common_ui_list_structure_preset.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.storyteller_f.sml")
    id("com.google.devtools.ksp")
    id("androidx.navigation.safeargs.kotlin")
    id("app.cash.licensee")
}
android {
    defaultConfig {
        applicationId = "com.storyteller_f.ping"
    }
    namespace = "com.storyteller_f.ping"
}

dependencies {
    implementation("com.google.android.exoplayer:exoplayer-core:2.18.1")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("com.squareup.okio:okio:3.2.0")
    implementation("com.github.bumptech.glide:glide:4.14.2")
}
baseApp()
setupGeneric()
setupDataBinding()
setupDipToPx()

licensee {
    allow("Apache-2.0")
    allow("MIT")
}