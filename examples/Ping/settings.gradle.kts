@file:Suppress("UnstableApiUsage")

pluginManagement {
//    includeBuild("../../../common-ui-list/version-manager")
//    includeBuild("../../../common-ui-list/common-publish")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            setUrl("https://jitpack.io")
        }
    }
}
//todo 生成脚本，自动完成此工作
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://artifactory.cronapp.io/public-release/") }
    }
}

rootProject.name = "Ping"
include(":ping")

val commonUiListModules = listOf<String>(

)
val commonUiPath = File(rootDir, "../../common-ui-list")
commonUiListModules.forEach {
    val modulePath = File(commonUiPath, it)
    if (modulePath.exists()) {
        include(it)
        project(":$it").projectDir = modulePath
    }
}
