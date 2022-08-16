import common_ui_list_structure_preset.*

plugins {
    id ("com.android.application")
    id ("org.jetbrains.kotlin.android")
    id ("kotlin-android")
}

android {

    defaultConfig {
        applicationId = "com.storyteller_f.b3"
    }

    buildFeatures {
        viewBinding = true
    }

    namespace = "com.storyteller_f.b3"
}

dependencies {
    implementation( fileTree("libs"))
    networkDependency()

    //geetest
    implementation ("com.geetest.sensebot:sensebot:4.3.5.3")
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation ("com.google.code.gson:gson:2.9.1")

}
baseApp()
setupGeneric()