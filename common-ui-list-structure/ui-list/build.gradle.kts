import com.storyteller_f.version_manager.*

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-android")
    id("kotlin-kapt")
    id("com.storyteller_f.version_manager")

}

android {
    defaultConfig {
        minSdk = 21
    }
    kotlinOptions {
        freeCompilerArgs = listOf("-opt-in=kotlin.RequiresOptIn")
    }

    buildFeatures {
        viewBinding = true
    }
    namespace = "com.storyteller_f.ui_list"
}

baseLibrary()

dependencies {
    implementation(project(":ui-list-annotation-compiler"))
    implementation(fileTree("libs"))

    //test
    unitTestDependency()

    //components
    api("androidx.appcompat:appcompat:${Versions.appcompatVersion}")
    api("androidx.recyclerview:recyclerview:${Versions.recyclerViewVersion}")
    api("androidx.constraintlayout:constraintlayout:${Versions.constraintLayoutVersion}")
    api("com.google.android.material:material:${Versions.materialVersion}")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    //kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutinesVersion}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutinesVersion}")

    // lifecycle & view model
    api("androidx.lifecycle:lifecycle-runtime-ktx:${Versions.lifecycleVersion}")
    api("androidx.lifecycle:lifecycle-viewmodel-ktx:${Versions.lifecycleVersion}")
    api("androidx.lifecycle:lifecycle-livedata-ktx:${Versions.lifecycleVersion}")
    api("androidx.lifecycle:lifecycle-viewmodel-savedstate:${Versions.lifecycleVersion}")

    // room
    api("androidx.room:room-runtime:${Versions.roomVersion}")
    api("androidx.room:room-ktx:${Versions.roomVersion}")
    api("androidx.room:room-paging:${Versions.roomVersion}")

    api("androidx.paging:paging-runtime-ktx:${Versions.pagingVersion}")

    kapt("androidx.databinding:databinding-compiler-common:${Versions.dataBindingCompilerVersion}")

    // retrofit & okhttp
    api("com.squareup.retrofit2:converter-gson:${Versions.retrofitVersion}")

    //ktx
    implementation("androidx.fragment:fragment-ktx:${Versions.fragmentKtxVersion}")
    implementation("androidx.activity:activity-ktx:${Versions.ktxVersion}")
    api(project(":common-vm-ktx"))
}