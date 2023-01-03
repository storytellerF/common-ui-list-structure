import common_ui_list_structure_preset.Versions

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.storyteller_f.plugin_core"
    compileSdk = 33

    defaultConfig {
        minSdk = 21
        targetSdk = 33

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:${Versions.coreVersion}")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("com.google.android.material:material:${Versions.materialVersion}")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.0")
}

tasks {
    val dispatchLib = register("dispatchPluginCoreLib", Copy::class) {
        from("~/AndroidStudioProjects/common-ui-list-structure/common-ui-list-structure/app/giant-explorer/plugin-core/build/outputs/aar/plugin-core-debug.aar") {
            rename {
                it.replace("plugin-core-debug.aar", "core.aar")
            }
        }
        into("~/AndroidStudioProjects/common-ui-list-structure/giant-explorer/yue/giant-explorer-plugin-core")
    }

    build {
        finalizedBy(dispatchLib)
    }
}