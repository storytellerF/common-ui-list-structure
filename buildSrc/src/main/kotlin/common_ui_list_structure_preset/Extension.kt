package common_ui_list_structure_preset

import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.dsl.DefaultConfig
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

fun DependencyHandlerScope.base() {
    "implementation"(project(":common-ktx"))
    "implementation"(project(":common-ui"))
    "implementation"(project(":ui-list"))
    "implementation"(project(":ui-list-annotation-definition"))
    "kapt"(project(":ui-list-annotation-compiler"))
    "implementation"(project(":composite-definition"))
    "kapt"(project(":composite-compiler"))

    "kapt"("androidx.room:room-compiler:${Versions.roomVersion}")

    "implementation"("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    "implementation"("androidx.constraintlayout:constraintlayout:${Versions.constraintLayoutVersion}")

    "implementation"("androidx.fragment:fragment-ktx:${Versions.ktxVersion}")
    "implementation"("androidx.activity:activity-ktx:${Versions.ktxVersion}")

    "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}")
    "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}")

    "debugImplementation"("com.squareup.leakcanary:leakcanary-android:2.9.1")
    "implementation"("androidx.multidex:multidex:2.0.1")
}

fun DependencyHandlerScope.composeSupport() {
    "implementation"(project(":view-holder-compose"))
    "implementation"("androidx.compose.material:material:${Versions.composeVersion}")
    "implementation"("androidx.compose.ui:ui-tooling:${Versions.composeVersion}")
}

fun DependencyHandlerScope.networkSupport() {
    "implementation"("com.squareup.retrofit2:retrofit:${Versions.retrofitVersion}")
    "implementation"("com.squareup.retrofit2:retrofit-mock:${Versions.retrofitVersion}")
    "implementation"("com.squareup.okhttp3:logging-interceptor:${Versions.okhttpLoggingInterceptorVersion}")
}

fun DependencyHandlerScope.navigationSupport() {
    "implementation"("androidx.navigation:navigation-fragment-ktx:${Versions.navVersion}")
    "implementation"("androidx.navigation:navigation-ui-ktx:${Versions.navVersion}")
}

fun DependencyHandlerScope.unitTestSupport() {
    "testImplementation"("junit:junit:4.13.2")
    "androidTestImplementation"("androidx.test.ext:junit:1.1.3")
    "androidTestImplementation"("androidx.test.espresso:espresso-core:3.4.0")
}

fun DependencyHandlerScope.extFuncSupport() {
    "ksp"(project(":ext-func-compiler"))
}

fun DependencyHandlerScope.dipToPx() {
    "implementation"(project(":common-pr"))
}
fun DependencyHandlerScope.dataBindingSupport() {
    "kapt"("androidx.databinding:databinding-compiler-common:7.2.2")
}

fun DependencyHandlerScope.worker() {
    "implementation"(project(":multi-core"))
    "implementation"( "androidx.work:work-runtime-ktx:${Versions.workVersion}")
    "androidTestImplementation"( "androidx.work:work-testing:${Versions.workVersion}")
    "implementation" ("androidx.work:work-multiprocess:${Versions.workVersion}")
}

fun DependencyHandlerScope.fileSystem() {
    "implementation"(project(":file-system"))
    "implementation"(project(":file-system-ktx"))
}

fun DependencyHandlerScope.generic() {
    base()
    extFuncSupport()
    composeSupport()
    navigationSupport()
    unitTestSupport()
}

fun Project.dataBinding() {
    android.run {
        buildFeatures {
            viewBinding = true
            dataBinding = true
        }
    }
    dependencies {
        dataBindingSupport()
    }
}

private val Project.android: BaseAppModuleExtension
    get() = extensions.findByName("android") as? BaseAppModuleExtension
        ?: error("Not an Android module: $name")