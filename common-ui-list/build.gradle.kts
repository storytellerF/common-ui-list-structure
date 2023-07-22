// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        val smlFolder: String by project
        val navVersion = "2.6.0"
        val smlVersion = "0.0.2"
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$navVersion")
        classpath("app.cash.licensee:licensee-gradle-plugin:1.6.0")
        when (smlFolder) {
            "remote" -> classpath("com.github.storytellerF.SML:com.storyteller_f.sml.gradle.plugin:$smlVersion")
            "repository" -> classpath("com.storyteller_f.sml:sml:$smlVersion")
        }
    }
}
plugins {
    val androidVersion = "8.0.2"
    val kotlinVersion = "1.8.21"
    val kspVersion = "1.8.21-1.0.11"
    id("com.android.application") version androidVersion apply false
    id("com.android.library") version androidVersion apply false
    id("org.jetbrains.kotlin.android") version kotlinVersion apply false
    id("org.jetbrains.kotlin.jvm") version kotlinVersion apply false
    id("com.google.devtools.ksp") version kspVersion apply false
    //使用includeBuild 时使用sml，不需要指定id
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
//subprojects {
//    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
//        kotlinOptions {
//            freeCompilerArgs = freeCompilerArgs + listOf("-Xlint:deprecation", "-Xlint:unchecked")
//        }
//    }
//    tasks.withType<JavaCompile> {
//        options.compilerArgs = options.compilerArgs + listOf("-Xlint:deprecation", "-Xlint:unchecked")
//    }
//
//}
