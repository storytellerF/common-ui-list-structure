pluginManagement {
    includeBuild("version-manager")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven {
            setUrl("https://jitpack.io")
        }
        maven {
            setUrl("../repo")
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
include(":app:b3")
include(":app:giant-explorer")
include(":file-system")
include(":file-system-ktx")
include(":multi-core")
include(":common-ktx")
include(":app:fapiao")
include(":common-pr")
include(":ext-func-compiler")
include(":ext-func-definition")
include(":app:ping")
include(":ui-list-annotation-compiler-ksp")
include(":ui-list-annotation-common")
include(":app:giant-explorer-plugin-core")
include(":slim-ktx")

val l = listOf("config-core", "filter-core", "sort-core", "config_edit", "filter-ui", "sort-ui", "recycleview_ui_extra")
val home: String = System.getProperty("user.home")
l.forEach {
    include("filter:$it")
    project(":filter:$it").projectDir = file("$home/AndroidStudioProjects/FilterUIProject/$it")
}