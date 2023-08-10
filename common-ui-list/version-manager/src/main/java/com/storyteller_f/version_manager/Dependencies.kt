@file:Suppress("unused")

package com.storyteller_f.version_manager

import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.project

object Versions {
    const val kotlinVersion = "1.8.21"
    const val agpVersion = "8.1.0"
    const val kspVersion = "1.8.21-1.0.11"
    const val compileSdkVersion = 33
    const val targetSdkVersion = 33
    const val appcompatVersion = "1.6.1"
    const val coreVersion = "1.10.1"
    const val recyclerViewVersion = "1.3.0"
    const val constraintLayoutVersion = "2.1.4"
    const val materialVersion = "1.9.0"
    const val activityKtxVersion = "1.7.2"
    const val fragmentKtxVersion = "1.6.0"
    const val lifecycleVersion = "2.6.1"
    const val roomVersion = "2.5.2"
    const val pagingVersion = "3.1.1"
    const val retrofitVersion = "2.9.0"
    const val okhttpLoggingInterceptorVersion = "5.0.0-alpha.6"
    const val coroutinesVersion = "1.6.4"
    const val composeCompilerVersion = "1.4.7"
    const val composeVersion = "1.4.0"
    const val composeUiVersion = "1.4.0"
    const val composeMaterialVersion = "1.4.0-beta02"
    const val navVersion = "2.5.1"
    const val workVersion = "2.7.1"
    const val leakCanary = "2.9.1"
    const val dataBindingCompilerVersion = "8.0.2"

}

object Libraries

/**
 * 需要加载kapt 插件
 */
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

/**
 * 需要kapt 插件
 */
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