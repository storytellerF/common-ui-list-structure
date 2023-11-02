buildscript {
    dependencies {
        val versionManager: String by project
        val navVersion = "2.7.4"
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$navVersion")
        //jitpack 构建
        classpath("com.github.storytellerF.common-ui-list:version-manager:$versionManager")
        //本地构建
//        classpath("com.storyteller_f:version-manager:0.0.1")
    }
}
plugins {
    val androidVersion = "8.3.0-alpha11"
    val kotlinVersion = "1.9.10"
    val kspVersion = "1.9.10-1.0.13"
    id("com.android.application") version androidVersion apply false
    id("com.android.library") version androidVersion apply false
    id("org.jetbrains.kotlin.android") version kotlinVersion apply false
    id("org.jetbrains.kotlin.jvm") version kotlinVersion apply false
    id("com.google.devtools.ksp") version kspVersion apply false
}