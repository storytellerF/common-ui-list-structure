@file:Suppress("unused", "UnstableApiUsage", "DEPRECATION")

package com.storyteller_f.version_manager

import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun Project.setupExtFunc() {
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

fun Project.setupCompose(isLibrary: Boolean = false, supportUiList: Boolean = true) {
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
        if (supportUiList) {
            "implementation"(project(":view-holder-compose"))
        }
    }
}

fun Project.setupBase() {
    loadPlugin("kotlin-kapt")
    dependencies {
        baseDependency()
    }
}

fun Project.setupGeneric() {
    setupBase()
    setupCompose()
    setupExtFunc()
    dependencies {
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
        kotlinOptionsApp {
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

        kotlinOptionsApp {
            jvmTarget = javaVersion.toString()
            addArgs("-opt-in=kotlin.RequiresOptIn")
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
            addArgs("-opt-in=kotlin.RequiresOptIn")
        }
    }
}

fun Project.androidLibrary(configure: Action<LibraryExtension>): Unit =
    (this as ExtensionAware).extensions.configure("android", configure)

fun Project.androidApp(configure: Action<BaseAppModuleExtension>): Unit =
    (this as ExtensionAware).extensions.configure("android", configure)

fun Project.kotlin(configure: Action<KotlinAndroidProjectExtension>): Unit =
    (this as ExtensionAware).extensions.configure("kotlin", configure)

val NamedDomainObjectContainer<KotlinSourceSet>.main: NamedDomainObjectProvider<KotlinSourceSet>
    get() = named<KotlinSourceSet>("main")

val NamedDomainObjectContainer<KotlinSourceSet>.test: NamedDomainObjectProvider<KotlinSourceSet>
    get() = named<KotlinSourceSet>("test")

fun BaseAppModuleExtension.kotlinOptionsApp(configure: Action<KotlinJvmOptions>): Unit =
    (this as ExtensionAware).extensions.configure("kotlinOptions", configure)

fun LibraryExtension.kotlinOptionsLibrary(configure: Action<KotlinJvmOptions>): Unit =
    (this as ExtensionAware).extensions.configure("kotlinOptions", configure)

fun Project.java(configure: Action<org.gradle.api.plugins.JavaPluginExtension>): Unit =
    (this as ExtensionAware).extensions.configure("java", configure)

fun KotlinJvmOptions.addArgs(arg: String) {
    freeCompilerArgs = freeCompilerArgs.plusIfNotExists(arg)
}

fun <T> List<T>.plusIfNotExists(element: T): List<T> {
    if (contains(element)) return this
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