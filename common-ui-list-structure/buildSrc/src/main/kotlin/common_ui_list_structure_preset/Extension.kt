package common_ui_list_structure_preset

import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

fun DependencyHandlerScope.baseDependency() {
    "implementation"(project(":common-ktx"))
    "implementation"(project(":common-ui"))
    "implementation"(project(":ui-list"))
    "implementation"(project(":ui-list-annotation-definition"))
    "kapt"(project(":ui-list-annotation-compiler"))
    "implementation"(project(":composite-definition"))
    "kapt"(project(":composite-compiler"))

    "kapt"("androidx.room:room-compiler:${Versions.roomVersion}")

//    "implementation"("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
//    "implementation"("androidx.constraintlayout:constraintlayout:${Versions.constraintLayoutVersion}")

    "implementation"("androidx.fragment:fragment-ktx:${Versions.ktxVersion}")
    "implementation"("androidx.activity:activity-ktx:${Versions.ktxVersion}")

    "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutinesVersion}")
    "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutinesVersion}")

    "debugImplementation"("com.squareup.leakcanary:leakcanary-android:${Versions.leakCanary}")
    "implementation"("androidx.multidex:multidex:2.0.1")
}

fun DependencyHandlerScope.composeDependency() {
    "implementation"("androidx.compose.material:material:${Versions.composeVersion}")
    "implementation"("androidx.compose.ui:ui-tooling:${Versions.composeVersion}")
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
    "kapt"("androidx.databinding:databinding-compiler-common:7.2.2")
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
        sourceSets.main {
            kotlin.srcDir("build/generated/ksp/main/kotlin")
        }
        sourceSets.test {
            kotlin.srcDir("build/generated/ksp/test/kotlin")
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
        android {
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
    android {
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
    android {
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
    android {
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
    android {
        compileSdk = Versions.compileSdkVersion
        defaultConfig {
            minSdk = 21
            versionCode = 1
            versionName = "1.0"
            targetSdk = Versions.targetSdkVersion
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
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
        kotlinOptionsLibrary {
            jvmTarget = "1.8"
        }
    }
}

fun Project.androidLibrary(configure: Action<com.android.build.gradle.LibraryExtension>): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("android", configure)

fun Project.android(configure: Action<BaseAppModuleExtension>): Unit =
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


fun <T> List<T>.plusWhenNoExists(element: T): List<T> {
    if (this.contains(element)) {
        return this
    }
    val result = ArrayList<T>(size + 1)
    result.addAll(this)
    result.add(element)
    return result
}