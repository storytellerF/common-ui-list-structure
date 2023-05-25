import com.storyteller_f.version_manager.pureKotlinLanguageLevel

plugins {
    kotlin("jvm")
    id("com.storyteller_f.version_manager")
}
pureKotlinLanguageLevel()

dependencies {
    implementation(project(":ui-list-annotation-definition"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.8.21-1.0.11")
    implementation(project(":ui-list-annotation-common"))
}