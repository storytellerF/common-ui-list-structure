pluginManagement {
    includeBuild("../../common-ui-list/version-manager")
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

val commonUiListModules = listOf(
    "common-ktx",
    "common-pr",
    "common-ui",
    "common-vm-ktx",
    "compat-ktx",
    "composite-compiler",
    "composite-definition",
    "config",
    "ext-func-compiler",
    "ext-func-definition",
    "file-system",
    "file-system-ktx",
    "file-system-remote",
    "file-system-root",
    "multi-core",
    "slim-ktx",
    "ui-list",
    "ui-list-annotation-common",
    "ui-list-annotation-compiler",
    "ui-list-annotation-compiler-ksp",
    "ui-list-annotation-definition",
    "view-holder-compose"
)
val commonUiPath = File(rootDir, "../../common-ui-list")
commonUiListModules.forEach {
    val modulePath = File(commonUiPath, it)
    include(it)
    project(":$it").projectDir = modulePath
}
