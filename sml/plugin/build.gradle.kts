plugins {
    id("java-gradle-plugin")
    id("maven-publish")
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        register("sml") {
            version = "0.0.2"
            // 插件ID
            id = "com.storyteller_f.sml"
            group = "com.storyteller_f.sml"
            // 插件的实现类
            implementationClass = "com.storyteller_f.sml.Sml"
        }
    }
}

publishing {
    repositories {
        maven {
            // $rootDir 表示你项目的根目录
            val file = File(rootDir, "../../repo")
            println(file)
            setUrl(file.absolutePath)
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
