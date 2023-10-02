plugins {
    id("java-gradle-plugin")
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("version-manager") {
            // 插件ID
            id = "com.storyteller_f.version_manager"
            // 插件的实现类
            implementationClass = "com.storyteller_f.version_manager.VersionManager"
        }
    }
}

dependencies {
    implementation("com.android.tools.build:gradle:8.1.2")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.21")
}