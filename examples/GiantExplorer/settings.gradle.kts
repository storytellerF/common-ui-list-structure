val filterFolder: String? by settings
val baoFolder: String? by settings
val liFolder: String? by settings
pluginManagement {
    includeBuild("../../common-ui-list/version-manager")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://artifactory.cronapp.io/public-release/") }
    }
}
rootProject.name = "GiantExplorer"
include(":giant-explorer")
include(":giant-explorer-plugin-core")

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

val home: String = System.getProperty("user.home")
val debugFilterFolder = file("$home/AndroidStudioProjects/FilterUIProject/")
val subModuleFilterFolder = file("../../FilterUIProject")
val currentFolder = when (filterFolder) {
    "local" -> debugFilterFolder
    "submodule" -> subModuleFilterFolder
    else -> null
}

if (currentFolder?.exists() == true) {
    val l = listOf("config-core", "filter-core", "sort-core", "config_edit", "filter-ui", "sort-ui", "recycleview_ui_extra")
    l.forEach {
        include("filter:$it")
        project(":filter:$it").projectDir = File(currentFolder, it)
    }
}

val debugBaoFolder = when (baoFolder) {
    "local" -> file("$home/AndroidStudioProjects/Bao/")
    else -> null
}
if (debugBaoFolder?.exists() == true) {
    val l = listOf("startup", "bao-library")
    for (sub in l) {
        include("bao:$sub")
        project(":bao:$sub").projectDir = File(debugBaoFolder, sub)
    }

}

val debugLiFolder = when (liFolder) {
    "local" -> file("../../giant-explorer/li/plugin")
    else -> null
}

if (debugLiFolder?.exists() == true) {
    include("li-plugin")
    project(":li-plugin").projectDir = debugLiFolder
}