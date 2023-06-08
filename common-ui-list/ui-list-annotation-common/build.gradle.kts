import com.storyteller_f.version_manager.pureKotlinLanguageLevel

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.storyteller_f.version_manager")
}
pureKotlinLanguageLevel()
dependencies {
    implementation(project(":slim-ktx"))
}