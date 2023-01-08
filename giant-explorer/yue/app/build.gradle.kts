import com.android.build.gradle.internal.tasks.factory.dependsOn
import java.util.regex.Pattern
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.storyteller_f.yue"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.storyteller_f.yue"
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
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.5.1")
    implementation("com.google.android.material:material:1.7.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.3")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.3")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.0")
    implementation(project(":yue-plugin"))
}
val dispatchApk = tasks.register("dispatchApk") {
    doLast {
        val userHome = System.getProperty("user.home")
        val adbPath = "$userHome/Library/Android/sdk/platform-tools/adb"
        val apkFile = "${buildDir.absolutePath}/outputs/apk/debug/app-debug.apk"
        val outputPath = "/data/data/com.storyteller_f.giant_explorer/files/plugins"
        val output = "$outputPath/yue.apk"
        val tmp = "/data/local/tmp/yue.apk"
        val getDevicesCommand = Runtime.getRuntime().exec(arrayOf(adbPath, "devices"))
        getDevicesCommand.waitFor()
        val readText = getDevicesCommand.inputStream.bufferedReader().use {
            it.readText().trim()
        }
        getDevicesCommand.destroy()
        val devices = readText.split("\n").let {
            it.subList(1, it.size)
        }.map {
            it.split(Pattern.compile("\\s+")).first()
        }
        devices.forEach {
            println("dispatch to $it")
            command(arrayOf(adbPath, "-s", it, "push", apkFile, tmp))
            command(arrayOf(adbPath, "-s", it, "shell", "run-as", "com.storyteller_f.giant_explorer", "sh", "-c", "\'mkdir $outputPath\'"))
            command(arrayOf(adbPath, "-s", it, "shell", "run-as", "com.storyteller_f.giant_explorer", "sh", "-c", "\'cp $tmp $output\'"))
            command(arrayOf(adbPath, "-s", it, "shell", "sh", "-c", "\'rm $tmp\'"))
        }
    }

}
afterEvaluate {
    val taskName = "packageDebug"
    val packageDebug = tasks.findByName(taskName)
    dispatchApk.dependsOn(taskName)
    dispatchApk.get().onlyIf {
        packageDebug?.state?.didWork == true
    }
    packageDebug?.finalizedBy(dispatchApk)
}

fun command(arrayOf: Array<String>): Int {
    val pushCommand = Runtime.getRuntime().exec(arrayOf)
    val waitFor = pushCommand.waitFor()
    pushCommand.destroy()
    return waitFor
}