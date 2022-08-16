import common_ui_list_structure_preset.Versions
import common_ui_list_structure_preset.baseLibrary
import common_ui_list_structure_preset.unitTestDependency
plugins {
    id ("com.android.library")
    id ("org.jetbrains.kotlin.android")
}

android {

    defaultConfig {
        minSdk = 16
    }

    namespace = "com.storyteller_f.file_system"
}
baseLibrary()
dependencies {
    implementation(project(":common-ktx"))

    implementation(project(":multi-core"))
    implementation ("androidx.core:core-ktx:${Versions.coreVersion}")
    implementation ("androidx.appcompat:appcompat:${Versions.appcompatVersion}")
    implementation ("com.google.android.material:material:${Versions.materialVersion}")
    implementation ("androidx.constraintlayout:constraintlayout:${Versions.constraintLayoutVersion}")
    unitTestDependency()

    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutinesVersion}")

}