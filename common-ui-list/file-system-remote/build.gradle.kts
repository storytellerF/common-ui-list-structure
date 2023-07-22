import com.storyteller_f.version_manager.Versions

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.storyteller_f.version_manager")

}

android {
    namespace = "com.storyteller_f.file_system_remote"
    compileSdk = 33

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    val javaVersion = JavaVersion.VERSION_1_8
    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    kotlinOptions {
        jvmTarget = javaVersion.toString()
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("androidx.room:room-common:${Versions.roomVersion}")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation(project(":file-system"))

    // https://mvnrepository.com/artifact/commons-net/commons-net
    implementation("commons-net:commons-net:3.9.0")
    // https://mvnrepository.com/artifact/org.mockftpserver/MockFtpServer
    testImplementation("org.mockftpserver:MockFtpServer:3.1.0")
    // https://mvnrepository.com/artifact/com.hierynomus/smbj
    implementation("com.hierynomus:smbj:0.11.5")
    // https://mvnrepository.com/artifact/com.hierynomus/sshj
    implementation("com.hierynomus:sshj:0.35.0")
}