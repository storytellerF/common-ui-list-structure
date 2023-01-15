@file:Suppress("unused")

package com.storyteller_f.sml

import com.android.build.api.variant.ApplicationVariant
import com.storyteller_f.sml.tasks.ColorTask
import com.storyteller_f.sml.tasks.DimensionTask
import com.storyteller_f.sml.tasks.DrawableDomain
import com.storyteller_f.sml.tasks.ShapeTask
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import java.io.File
import java.util.*

interface SmlExtension {
    val color: MapProperty<String, String>
    val dimen: MapProperty<String, String>
    val drawables: NamedDomainObjectContainer<DrawableDomain>
}

class Sml : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("sml", SmlExtension::class.java)
        val rootPath = "${project.buildDir}/generated"
        project.androidComponents { componentsExtension ->
            componentsExtension.onVariants { variant ->
                setup(variant, project, rootPath, extension)
            }
        }
    }

    private fun setup(
        variant: ApplicationVariant,
        project: Project,
        rootPath: String,
        extension: SmlExtension
    ) {
        val subPath = variant.name
        val variantName = variant.name
        val buildType = variant.name
        project.tasks.register(taskName("Colors", variantName), ColorTask::class.java) {
            val colorsOutputDirectory =
                File(File(rootPath, "sml_res_colors"), subPath).apply { mkdirs() }
            it.config(colorsOutputDirectory, extension, project, buildType)
        }

        project.tasks.register(taskName("Dimens", variantName), DimensionTask::class.java) {
            val dimensOutputDirectory =
                File(File(rootPath, "sml_res_dimens"), subPath).apply { mkdirs() }
            it.config(dimensOutputDirectory, extension, project, buildType)
        }

        project.tasks.register(taskName("Shapes", variantName), ShapeTask::class.java) {
            val drawablesOutputDirectory =
                File(File(rootPath, "sml_res_drawables"), subPath).apply { mkdirs() }
            it.config(drawablesOutputDirectory, extension, project, buildType)
        }
    }

    private fun ShapeTask.config(drawablesOutputDirectory: File, extension: SmlExtension, project: Project, buildType: String) {
        group = "sml"
        val path = File(drawablesOutputDirectory, "drawable")
        outputDirectory = path
        outputFile = extension.drawables.map { shapeDomain ->
            File(path, "${shapeDomain.name}.xml")
        }.toTypedArray()
        drawableDomain = extension.drawables.toTypedArray()
        bindOutputPath(project, drawablesOutputDirectory, buildType)
    }

    private fun DimensionTask.config(dimensOutputDirectory: File, extension: SmlExtension, project: Project, buildType: String) {
        group = "sml"
        outputFile = File(dimensOutputDirectory, "values/generated_dimens.xml")
        dimensMap = extension.dimen.get()
        bindOutputPath(project, dimensOutputDirectory, buildType)
    }

    private fun ColorTask.config(colorsOutputDirectory: File, extension: SmlExtension, project: Project, buildType: String) {
        group = "sml"
        outputFile = File(colorsOutputDirectory, "values/generated_colors.xml")
        colorsMap = extension.color.get()
        bindOutputPath(project, colorsOutputDirectory, buildType)
    }

    private fun bindOutputPath(project: Project, outputDirectory: File, buildType: String) {
//        val substring = outputDirectory.absolutePath.substring(project.projectDir.absolutePath.length + 1)
//        project.kotlin {
//            it.sourceSets.getByName(buildType) { sourceSet ->
//                sourceSet.kotlin.srcDirs(substring)
//            }
//        }

    }

    private fun taskName(type: String, variantName: String): String {
        val name = variantName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        return "generate$type$name"
    }
}