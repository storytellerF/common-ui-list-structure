plugins {
    kotlin("jvm") version "1.8.0"
    application
}

group = "org.storyteller_f"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.github.storytellerF.song:plugin:1.2")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("MainKt")
}
