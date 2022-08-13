import com.storyteller_f.sml.Line
import com.storyteller_f.sml.Oval
import com.storyteller_f.sml.Rectangle
import com.storyteller_f.sml.Ring
import common_ui_list_structure_preset.navigationSupport
import common_ui_list_structure_preset.unitTestSupport

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.storyteller_f.sml")
}
android {
    compileSdk = 32

    defaultConfig {
        applicationId = "com.storyteller_f.ping"
        minSdkPreview = "21"
        targetSdkPreview = "32"
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.appcompat:appcompat:1.4.2")
    implementation("com.google.android.material:material:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    navigationSupport()
    implementation("com.google.android.exoplayer:exoplayer-core:2.18.1")
    unitTestSupport()
}

sml {
    color.set(mutableMapOf("test" to "#ff0000"))
    dimen.set(mutableMapOf("test1" to "12"))
    drawables {
        register("hello") {
            Rectangle {
                solid("#00ff00")
                corners("12dp")
            }
        }
        register("test") {
            Oval {
                solid("#ff0000")
            }
        }
        register("test1") {
            Ring("10dp", "1dp") {
                ring("#ffff00", "10dp")
            }
        }
        register("test2") {
            Line {
                line("#ff0000", "10dp")
            }
        }
    }
}