import common_ui_list_structure_preset.*

plugins {
    id ("com.android.application")
    id ("org.jetbrains.kotlin.android")
    id ("kotlin-android")
    id ("kotlin-kapt")
    id ("com.google.devtools.ksp")
}

android {
    compileSdk = Versions.compileSdkVersion

    defaultConfig {
        applicationId = "com.storyteller_f.b3"
        minSdk = 21
        targetSdk = Versions.targetSdkVersion
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles (getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility= JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs =listOf ("-opt-in=kotlin.RequiresOptIn")
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Versions.composeCompilerVersion
    }
    namespace = "com.storyteller_f.b3"
    dependenciesInfo {
        includeInBundle = false
        includeInApk = false
    }
}

dependencies {
    implementation( fileTree("libs"))
    generic()
    networkSupport()

    //geetest
    implementation ("com.geetest.sensebot:sensebot:4.3.5.3")
    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation ("com.google.code.gson:gson:2.9.1")

}