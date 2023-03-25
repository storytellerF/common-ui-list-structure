plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.storyteller_f.yue_plugin"
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

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    api(project(":giant-explorer-plugin-core"))
    api("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
}
//val unpackAar = tasks.register<Copy>("unpackAar") {
//    from(zipTree(layout.buildDirectory.file("outputs/aar/yue-plugin-debug.aar")))
//    into(layout.buildDirectory.dir("intermediates/aar_unzip"))
//}
//val convertJarToDex = tasks.register<Exec>("convertJarToDex") {
//    workingDir = File(buildDir, "intermediates/aar_unzip")
//    commandLine = listOf("~/Library/Android/sdk/build-tools/29.0.1/dx", "--dex", "--output=classes.dex", "$buildDir/intermediates/aar_unzip/classes.jar")
//}
//val packGep = tasks.register<Zip>("packGep") {
//    archiveFileName.set("yue.gep")
//    exclude("*.jar")
//    destinationDirectory.set(layout.buildDirectory.dir("outputs/gep"))
//    from(layout.buildDirectory.dir("intermediates/aar_unzip"))
//}
//
//packGep.dependsOn(convertJarToDex)
//convertJarToDex.dependsOn(unpackAar)
//unpackAar.dependsOn("bundleDebugAar")
//tasks.build {
//    finalizedBy(packGep)
//}