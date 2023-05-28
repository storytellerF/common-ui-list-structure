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
    implementation("com.github.storytellerF.song:plugin:2.0")
    implementation("ch.qos.logback:logback-classic:1.2.9")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation("org.slf4j:slf4j-api:1.7.25")
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
