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
    implementation("com.android.tools.build:gradle:7.4.2")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.21")
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:1.7.21-1.0.8")
}