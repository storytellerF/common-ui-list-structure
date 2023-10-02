@file:Suppress("UnstableApiUsage")

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.report.ReportMergeTask

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
    val androidVersion = "8.1.2"
    val kotlinVersion = "1.8.21"
    val kspVersion = "1.8.21-1.0.11"
    id("com.android.application") version androidVersion apply false
    id("com.android.library") version androidVersion apply false
    id("org.jetbrains.kotlin.android") version kotlinVersion apply false
    id("org.jetbrains.kotlin.jvm") version kotlinVersion apply false
    id("com.google.devtools.ksp") version kspVersion apply false
    //使用includeBuild 时使用sml，不需要指定id
    id("io.gitlab.arturbosch.detekt") version ("1.23.1")
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

val deprecationCheckModule = listOf("common-ktx")
subprojects {
    if (deprecationCheckModule.contains(name)) {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions {
                freeCompilerArgs = freeCompilerArgs + listOf("-Xlint:deprecation", "-Xlint:unchecked")
            }
        }
        tasks.withType<JavaCompile> {
            options.compilerArgs = options.compilerArgs + listOf("-Xlint:deprecation", "-Xlint:unchecked")
        }
    }
}

val detektReportMergeSarif by tasks.registering(ReportMergeTask::class) {
    output = layout.buildDirectory.file("reports/detekt/merge.sarif")
}

allprojects {

    apply(plugin = "io.gitlab.arturbosch.detekt")

    detekt {
        source.setFrom(
            io.gitlab.arturbosch.detekt.extensions.DetektExtension.DEFAULT_SRC_DIR_JAVA,
            io.gitlab.arturbosch.detekt.extensions.DetektExtension.DEFAULT_TEST_SRC_DIR_JAVA,
            io.gitlab.arturbosch.detekt.extensions.DetektExtension.DEFAULT_SRC_DIR_KOTLIN,
            io.gitlab.arturbosch.detekt.extensions.DetektExtension.DEFAULT_TEST_SRC_DIR_KOTLIN,
        )
        buildUponDefaultConfig = true
        autoCorrect = true
        config.setFrom("$rootDir/config/detekt/detekt.yml")
        baseline = file("$rootDir/config/detekt/baseline.xml")
    }

    dependencies {
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.1")
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-libraries:1.23.1")
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-rules-ruleauthors:1.23.1")
    }

    tasks.withType<Detekt>().configureEach {
        jvmTarget = "1.8"
        reports {
            xml.required = true
            html.required = true
            txt.required = true
            sarif.required = true
            md.required = true
        }
        basePath = rootDir.absolutePath
        finalizedBy(detektReportMergeSarif)
    }
    detektReportMergeSarif {
        input.from(tasks.withType<Detekt>().map { it.sarifReportFile })
    }
    tasks.withType<DetektCreateBaselineTask>().configureEach {
        jvmTarget = "1.8"
    }
}