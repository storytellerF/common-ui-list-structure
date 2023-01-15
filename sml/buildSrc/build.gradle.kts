plugins {
    kotlin("jvm") version "1.7.21"
    id("java-gradle-plugin")
    id("maven-publish")
}

repositories {
    mavenCentral()
    google()
}

gradlePlugin {
    plugins {
        // 声明插件信息，这里的 hello 名字随意
        register("song") {
            version = "0.0.1"
            // 插件ID
            id = "com.storyteller_f.sml"
            // 插件的实现类
            implementationClass = "com.storyteller_f.sml.Sml"
        }
    }
}

publishing {
    repositories {
        maven {
            // $rootDir 表示你项目的根目录
            setUrl("$rootDir/repo-snapshot")
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.7.21")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.21")

    // Android gradle plugin will allow us to access Android specific features
    implementation("com.android.tools.build:gradle:7.4.0")
}