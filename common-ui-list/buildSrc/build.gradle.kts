plugins {
    `kotlin-dsl`
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.21")
    }
}

repositories {
    mavenCentral()
    google()
}

dependencies {
}