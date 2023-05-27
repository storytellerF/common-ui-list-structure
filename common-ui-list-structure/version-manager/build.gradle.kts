plugins {
//    id("java-library")
    id("java-gradle-plugin")
    `kotlin-dsl`
}

//val javaVersion = JavaVersion.VERSION_17
//java {
//    sourceCompatibility = javaVersion
//    targetCompatibility = javaVersion
//}
//tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
//    kotlinOptions.jvmTarget = javaVersion.toString()
//}

gradlePlugin {
    plugins {
        register("song") {
            // 插件ID
            id = "com.storyteller_f.version_manager"
            // 插件的实现类
            implementationClass = "com.storyteller_f.version_manager.VersionManager"
        }
    }
}

dependencies {
    implementation("com.android.tools.build:gradle:8.0.0-rc01")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
}