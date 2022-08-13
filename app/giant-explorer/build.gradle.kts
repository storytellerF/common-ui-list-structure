import common_ui_list_structure_preset.*

plugins {
    id ("com.android.application")
    id ("org.jetbrains.kotlin.android")
    id ("kotlin-kapt")
    id ("kotlin-parcelize")
    id ("androidx.navigation.safeargs.kotlin")
    id ("com.google.devtools.ksp")
}

android {
    compileSdk = Versions.compileSdkVersion

    defaultConfig {
        applicationId = "com.storyteller_f.giant_explorer"
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
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Versions.composeCompilerVersion
    }
    namespace = "com.storyteller_f.giant_explorer"

    defaultConfig {
        val fileProvider = "$applicationId.file-provider"

        // Now we can use ${documentsAuthority} in our Manifest
        manifestPlaceholders["providerAuthority"] = fileProvider
        // Now we can use BuildConfig.DOCUMENTS_AUTHORITY in our code
        buildConfigField( "String",
                "File_PROVIDER_AUTHORITY",
                "\"${fileProvider}\"")
    }
    dependenciesInfo {
        includeInBundle = false
        includeInApk = false
    }
}

dependencies {
    implementation (fileTree("libs"))
    generic()
    fileSystem()
    networkSupport()
    worker()
    implementation ("androidx.preference:preference:1.2.0")
    implementation ("com.j256.simplemagic:simplemagic:1.17")
    implementation ("com.github.osama-raddad:FireCrasher:2.0.0")

}
dataBinding()

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}