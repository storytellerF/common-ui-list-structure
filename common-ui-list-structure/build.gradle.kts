// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        val navVersion = "2.4.2"
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$navVersion")
        classpath("app.cash.licensee:licensee-gradle-plugin:1.6.0")
    }
}
plugins {
    id("com.storyteller_f.sml") version ("0.0.1") apply false
    id("io.gitlab.arturbosch.detekt") version ("1.21.0")
}
dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.16.0")
}
val projectSource = file(projectDir)
val configFile = files("$rootDir/config/detekt/detekt.yml")
//def baselineFile = file("$rootDir/config/detekt/baseline.xml")
val kotlinFiles = "**/*.kt"
val resourceFiles = "**/resources/**"
val buildFiles = "**/build/**"

tasks.register<io.gitlab.arturbosch.detekt.Detekt>("detektAll") {
    val autoFix = project.hasProperty("detektAutoFix")

    description = "Custom DETEKT build for all modules"
    parallel = true
    ignoreFailures = false
    autoCorrect = autoFix
    buildUponDefaultConfig = true
    setSource(projectSource)
//    baseline.set(baselineFile)
    config.setFrom(configFile)
    include(kotlinFiles)
    exclude(resourceFiles, buildFiles)
    reports {
        html.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

allprojects {
    gradle.projectsEvaluated {
        tasks.withType<JavaCompile> {
            options.compilerArgs.plusAssign(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
        }
    }
}