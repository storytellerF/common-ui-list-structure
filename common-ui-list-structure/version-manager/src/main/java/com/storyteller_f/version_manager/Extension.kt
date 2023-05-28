@file:Suppress("unused", "UnstableApiUsage", "DEPRECATION")

package com.storyteller_f.version_manager

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun DependencyHandlerScope.baseDependency() {
    "implementation"(project(":common-ktx"))
    "implementation"(project(":common-ui"))
    "implementation"(project(":ui-list"))
    "implementation"(project(":ui-list-annotation-definition"))
    "kapt"(project(":ui-list-annotation-compiler"))
    "implementation"(project(":composite-definition"))
    "kapt"(project(":composite-compiler"))

    "kapt"("androidx.room:room-compiler:${Versions.roomVersion}")

    "implementation"("androidx.fragment:fragment-ktx:${Versions.fragmentKtxVersion}")
    "implementation"("androidx.activity:activity-ktx:${Versions.activityKtxVersion}")

    "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutinesVersion}")
    "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutinesVersion}")

    "debugImplementation"("com.squareup.leakcanary:leakcanary-android:${Versions.leakCanary}")
    "implementation"("androidx.multidex:multidex:2.0.1")
}

fun DependencyHandlerScope.composeDependency() {
    "implementation"("androidx.compose.material:material:${Versions.composeMaterialVersion}")
    "implementation"("androidx.compose.ui:ui-tooling:${Versions.composeUiVersion}")
}

fun DependencyHandlerScope.networkDependency() {
    "implementation"("com.squareup.retrofit2:retrofit:${Versions.retrofitVersion}")
    "implementation"("com.squareup.retrofit2:retrofit-mock:${Versions.retrofitVersion}")
    "implementation"("com.squareup.okhttp3:logging-interceptor:${Versions.okhttpLoggingInterceptorVersion}")
}

fun DependencyHandlerScope.navigationDependency() {
    "implementation"("androidx.navigation:navigation-fragment-ktx:${Versions.navVersion}")
    "implementation"("androidx.navigation:navigation-ui-ktx:${Versions.navVersion}")
}

fun DependencyHandlerScope.unitTestDependency() {
    "testImplementation"("junit:junit:4.13.2")
    "androidTestImplementation"("androidx.test.ext:junit:1.1.3")
    "androidTestImplementation"("androidx.test.espresso:espresso-core:3.4.0")
}

fun DependencyHandlerScope.dipToPxDependency() {
    "implementation"(project(":common-pr"))
}

fun DependencyHandlerScope.dataBindingDependency() {
    "kapt"("androidx.databinding:databinding-compiler-common:${Versions.dataBindingCompilerVersion}")
}

fun DependencyHandlerScope.workerDependency() {
    "implementation"(project(":multi-core"))
    "implementation"("androidx.work:work-runtime-ktx:${Versions.workVersion}")
    "androidTestImplementation"("androidx.work:work-testing:${Versions.workVersion}")
    "implementation"("androidx.work:work-multiprocess:${Versions.workVersion}")
}

fun DependencyHandlerScope.fileSystemDependency() {
    "implementation"(project(":file-system"))
    "implementation"(project(":file-system-ktx"))
}

fun Project.setupExtFuncSupport() {
    loadPlugin("com.google.devtools.ksp")
    dependencies {
        "ksp"(project(":ext-func-compiler"))
    }
    kotlin {
        sourceSets {
            getByName("debug") {
                kotlin.srcDir("build/generated/ksp/debug/kotlin")
            }
        }
    }
}

fun Project.setupCompose(isLibrary: Boolean) {
    if (isLibrary) {
        androidLibrary {
            buildFeatures {
                compose = true
            }
            composeOptions {
                kotlinCompilerExtensionVersion = Versions.composeCompilerVersion
            }
        }
    } else
        androidApp {
            buildFeatures {
                compose = true
            }
            composeOptions {
                kotlinCompilerExtensionVersion = Versions.composeCompilerVersion
            }
        }
    dependencies {
        composeDependency()
    }
}

fun Project.setupGeneric() {
    setupCompose(false)
    setupExtFuncSupport()
    loadPlugin("kotlin-kapt")
    androidApp {
        kotlinOptions {
            addArgs("-opt-in=kotlin.RequiresOptIn")
        }
    }
    dependencies {
        "implementation"(project(":view-holder-compose"))
        baseDependency()
        navigationDependency()
        unitTestDependency()
    }
}

fun Project.setupDataBinding() {
    loadPlugin("kotlin-kapt")
    androidApp {
        buildFeatures {
            viewBinding = true
            dataBinding = true
        }
    }
    dependencies {
        dataBindingDependency()
    }
}

fun Project.setupDipToPx() {
    androidApp {
        kotlinOptions {
            addArgs("-Xcontext-receivers")
        }
    }
    dependencies {
        dipToPxDependency()
    }
}

fun Project.loadPlugin(id: String) {
    if (!plugins.hasPlugin(id)) plugins.apply(id)
}

fun Project.baseApp() {
//    listOf("com.android.application", "org.jetbrains.kotlin.android", "kotlin-android", "kotlin-parcelize").forEach {
//        loadPlugin(it)
//    }
    androidApp {
        compileSdk = Versions.compileSdkVersion
        defaultConfig {
            minSdk = 21
            versionCode = 1
            versionName = "1.0"
            targetSdk = Versions.targetSdkVersion
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        signingConfigs {
            val path = System.getenv("storyteller_f_sign_path")
            val alias = System.getenv("storyteller_f_sign_alias")
            val storePassword = System.getenv("storyteller_f_sign_store_password")
            val keyPassword = System.getenv("storyteller_f_sign_key_password")
            if (path != null && alias != null && storePassword != null && keyPassword != null) {
                create("release") {
                    keyAlias = alias
                    this.keyPassword = keyPassword
                    storeFile = file(path)
                    this.storePassword = storePassword
                }
            }
        }
        buildTypes {
            release {
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                val releaseSignConfig = signingConfigs.findByName("release")
                if (releaseSignConfig != null)
                    signingConfig = releaseSignConfig
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
        dependenciesInfo {
            includeInBundle = false
            includeInApk = false
        }
    }
}

fun Project.baseLibrary() {
    androidLibrary {
        compileSdk = Versions.compileSdkVersion

        defaultConfig {
            targetSdk = Versions.targetSdkVersion
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            consumerProguardFiles("consumer-rules.pro")
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
        kotlinOptionsLibrary {
            jvmTarget = javaVersion.toString()
        }
    }
}

fun Project.androidLibrary(configure: Action<com.android.build.gradle.LibraryExtension>): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("android", configure)

fun Project.androidApp(configure: Action<BaseAppModuleExtension>): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("android", configure)

fun Project.kotlin(configure: Action<KotlinAndroidProjectExtension>): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("kotlin", configure)

val org.gradle.api.NamedDomainObjectContainer<KotlinSourceSet>.main: NamedDomainObjectProvider<KotlinSourceSet>
    get() = named<KotlinSourceSet>("main")

val org.gradle.api.NamedDomainObjectContainer<KotlinSourceSet>.test: NamedDomainObjectProvider<KotlinSourceSet>
    get() = named<KotlinSourceSet>("test")

fun BaseAppModuleExtension.kotlinOptions(configure: Action<KotlinJvmOptions>): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("kotlinOptions", configure)

fun KotlinJvmOptions.addArgs(arg: String) {
    freeCompilerArgs = freeCompilerArgs.plusWhenNoExists(arg)
}

fun com.android.build.gradle.LibraryExtension.kotlinOptionsLibrary(configure: Action<KotlinJvmOptions>): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("kotlinOptions", configure)

fun Project.java(configure: Action<org.gradle.api.plugins.JavaPluginExtension>): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("java", configure)

fun <T> List<T>.plusWhenNoExists(element: T): List<T> {
    if (this.contains(element)) {
        return this
    }
    val result = ArrayList<T>(size + 1)
    result.addAll(this)
    result.add(element)
    return result
}

fun Project.pureKotlinLanguageLevel() {
    val javaVersion = JavaVersion.VERSION_17
    java {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = javaVersion.toString()
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }
}