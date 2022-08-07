plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android")
    id("kotlin-parcelize")
    id("kotlin-kapt")
    id("com.google.devtools.ksp")
    id("java-library-convention")
}

android {
    compileSdk = 32

    defaultConfig {
        applicationId = "com.storyteller_f.common_ui_list_structure"
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
        freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn", "-Xcontext-receivers")
    }

    buildFeatures {
        compose = true
        dataBinding = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.3.0-beta01"
    }
    namespace = "com.storyteller_f.common_ui_list_structure"
    dependenciesInfo {
        includeInBundle = false
        includeInApk = false
    }
}

dependencies {
    ksp(project(":ext-func-compiler"))
    implementation(project(":common-pr"))
    implementation(project(":ui-list"))
    implementation(project(":ui-list-annotation-definition"))
    implementation(project(":composite-definition"))
    implementation(project(":common-ui"))
    implementation("androidx.navigation:navigation-fragment-ktx:2.5.1")
    implementation("androidx.navigation:navigation-ui-ktx:2.5.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    kapt(project(":ui-list-annotation-compiler"))
    kapt(project(":composite-compiler"))
    implementation(project(":view-holder-compose"))

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")


    implementation(fileTree("libs"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // architecture components
    kapt("androidx.room:room-compiler:2.4.3")
    kapt("androidx.databinding:databinding-compiler-common:7.2.2")


    // retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:retrofit-mock:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.6")

    //view model
    implementation("androidx.fragment:fragment-ktx:1.5.1")
    implementation("androidx.activity:activity-ktx:1.5.1")

    implementation("androidx.multidex:multidex:2.0.1")

    //compose
    implementation("androidx.compose.material:material:${Versions.composeVersion}")
    implementation("androidx.compose.ui:ui-tooling:${Versions.composeVersion}")
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.9.1")

}

kotlin {
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
    sourceSets.test {
        kotlin.srcDir("build/generated/ksp/test/kotlin")
    }
}