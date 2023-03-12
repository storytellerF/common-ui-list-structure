import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "11"
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

dependencies {
    implementation(project(":ui-list-annotation-definition"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.8.0-1.0.9")
    implementation(project(":ui-list-annotation-common"))
}