import common_ui_list_structure_preset.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("androidx.navigation.safeargs.kotlin")
//    id("app.cash.licensee")
    id("com.storyteller_f.sml")
}
android {
    defaultConfig {
        applicationId = "com.storyteller_f.ping"
    }
    namespace = "com.storyteller_f.ping"
}

dependencies {
    implementation("com.google.android.exoplayer:exoplayer-core:2.18.4")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("com.squareup.okio:okio:3.3.0")
    implementation("com.github.bumptech.glide:glide:4.15.0")
    "implementation"(project(":file-system-ktx"))
}
baseApp()
setupGeneric()
setupDataBinding()
setupDipToPx()

//licensee {
//    allow("Apache-2.0")
//    allow("MIT")
//    allow("ISC")
//}

sml {
    
}