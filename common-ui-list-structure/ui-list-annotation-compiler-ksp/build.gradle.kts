import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

dependencies {
    implementation(project(":ui-list-annotation-definition"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.7.20-Beta-1.0.6")
    implementation(project(":ui-list-annotation-common"))
}