buildscript {
    dependencies {
        val smlFolder: String? by project
        val smlVersion = "0.0.2"
        val navVersion = "2.6.0"

        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$navVersion")
        classpath("com.github.storytellerF.common-ui-list:version-manager:cbf40f5ff3")
        when (smlFolder) {
            "remote", null -> classpath("com.github.storytellerF.SML:com.storyteller_f.sml.gradle.plugin:$smlVersion")
            "repository" -> classpath("com.storyteller_f.sml:sml:$smlVersion")
        }
    }
}
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    val androidVersion = "8.1.2"
    val kotlinVersion = "1.8.21"
    val kspVersion = "1.8.21-1.0.11"
    id("com.android.application") version androidVersion apply false
    id("com.android.library") version androidVersion apply false
    id("org.jetbrains.kotlin.android") version kotlinVersion apply false
    id("org.jetbrains.kotlin.jvm") version kotlinVersion apply false
    id("com.google.devtools.ksp") version kspVersion apply false
}