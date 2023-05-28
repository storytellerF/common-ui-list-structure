import com.storyteller_f.sml.config.*
import com.storyteller_f.sml.dimens
import com.storyteller_f.sml.reference

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.storyteller_f.sml")
}

android {
    namespace = "com.storyteller_f.sml"
    compileSdk = 33
    defaultConfig {
        applicationId = "com.storyteller_f.sml"
        minSdk = 21
        targetSdk = 33
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
    val javaVersion = JavaVersion.VERSION_17
    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    kotlinOptions {
        jvmTarget = javaVersion.toString()
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

interface DD {
    val rectRadius
            : Dimension
}

class Test : DD {
    override val rectRadius: Dimension
        get() = Dp(12f)
}

sml {
    color.set(mutableMapOf("test" to "#ff0000"))
    dimen.set(Test::class.dimens())
    drawables {
        register("hello") {
            Rectangle {
                solid(RgbColor("#00ff00"))
                corners(Test::rectRadius.reference())
            }
        }
        register("test") {
            Oval {
                solid(RgbColor("#00ff00"))
            }
        }
        register("test1") {
            Ring("10dp", "1dp") {
                ring(RgbColor("#00ff00"), Dp(10f))
            }
        }
        register("test2") {
            Line {
                line(RgbColor("#00ff00"), Dp(10f))
            }
        }
    }
}
val smlTargetPath = listOf("sml_res_colors", "sml_res_dimens", "sml_res_drawables")
val debugPath = smlTargetPath.map {
    "build/generated/$it/"
}
val type = listOf("debug", "release")
kotlin {
    sourceSets {
        sourceSets.forEach {
            println(it.name)
        }
        type.forEach {
            getByName(it) {
                kotlin.srcDirs(debugPath.map { p ->
                    "$p$it"
                }.toTypedArray())
            }
        }
    }
}