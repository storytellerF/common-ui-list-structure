plugins {
    id("java-library")
    id("java-gradle-plugin")
    `kotlin-dsl`
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
}

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
    implementation("com.android.tools.build:gradle:7.4.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.21")
}