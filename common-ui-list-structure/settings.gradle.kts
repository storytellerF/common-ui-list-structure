val filterFolder: String by settings
val baoFolder: String by settings
pluginManagement {
    val smlFolder: String by settings
    includeBuild("version-manager")
    if (smlFolder == "submodule" || smlFolder == "local") {
        includeBuild("../SML/plugin")
    }
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven {
            setUrl("https://jitpack.io")
        }
        if (smlFolder == "repository") {
            maven {
                setUrl("../repo")
            }
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        maven { setUrl("https://artifactory.cronapp.io/public-release/") }
    }
}
rootProject.name = "common_ui_list_structure"
include(":app")
include(":ui-list-annotation-compiler")
include(":common-vm-ktx")
include(":ui-list")
include(":view-holder-compose")
include(":ui-list-annotation-definition")
include(":common-ui")
include(":composite-definition")
include(":composite-compiler")
include(":app:giant-explorer")
include(":file-system")
include(":file-system-ktx")
include(":multi-core")
include(":common-ktx")
include(":common-pr")
include(":ext-func-compiler")
include(":ext-func-definition")
include(":app:ping")
include(":ui-list-annotation-compiler-ksp")
include(":ui-list-annotation-common")
include(":app:giant-explorer-plugin-core")
include(":slim-ktx")

val home: String = System.getProperty("user.home")
val debugFilterFolder = file("$home/AndroidStudioProjects/FilterUIProject/")
val subModuleFilterFolder = file("./FilterUIProject")
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
include(":compat-ktx")
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

