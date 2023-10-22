import com.storyteller_f.version_manager.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("androidx.navigation.safeargs.kotlin")
//    id("app.cash.licensee")
    id("com.storyteller_f.sml")
    id("com.storyteller_f.version_manager")
    id("kotlin-kapt")
}
android {
    defaultConfig {
        applicationId = "com.storyteller_f.ping"
    }
    namespace = "com.storyteller_f.ping"
}

dependencies {
    implementation("com.google.android.exoplayer:exoplayer-core:2.19.1")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("com.squareup.okio:okio:3.5.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implModule(":file-system-ktx")
}
constraintCommonUIListVersion("cbf40f5ff3")
baseApp()
setupGeneric()
setupDataBinding()
setupPreviewFeature()

//licensee {
//    allow("Apache-2.0")
//    allow("MIT")
//    allow("ISC")
//}

sml {
    
}
