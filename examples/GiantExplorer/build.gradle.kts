buildscript {
    dependencies {
        val navVersion = "2.6.0"
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$navVersion")
        //jitpack 构建
        classpath("com.github.storytellerF.common-ui-list:version-manager:cbf40f5ff3")
        //本地构建
//        classpath("com.storyteller_f:version-manager:0.0.1")
    }
}
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