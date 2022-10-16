@file:Suppress("unused")

package com.storyteller_f.sml

import com.android.build.gradle.api.BaseVariant
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
        project.android().variants().all { variant ->
            val subPath = variant.dirName
            val colorsOutputDirectory =
                File(File(rootPath, "sml_res_colors"), subPath).apply { mkdirs() }
            val dimensOutputDirectory =
                File(File(rootPath, "sml_res_dimens"), subPath).apply { mkdirs() }
            val drawablesOutputDirectory =
                File(File(rootPath, "sml_res_drawables"), subPath).apply { mkdirs() }
            project.tasks.register(taskName(variant, "Colors"), ColorTask::class.java) {
                config(it, colorsOutputDirectory, extension, variant, project)
            }

            project.tasks.register(taskName(variant, "Dimens"), DimensionTask::class.java) {
                config(it, dimensOutputDirectory, extension, variant, project)
            }

            project.tasks.register(taskName(variant, "Shapes"), ShapeTask::class.java) {
                config(it, drawablesOutputDirectory, extension, variant, project)
            }

        }

    }

    private fun config(
        it: ShapeTask,
        drawablesOutputDirectory: File,
        extension: SmlExtension,
        variant: BaseVariant,
        project: Project
    ) {
        it.group = "sml"
        val path = File(drawablesOutputDirectory, "drawable")
        it.outputDirectory = path
        it.outputFile = extension.drawables.map { shapeDomain ->
            File(path, "${shapeDomain.name}.xml")
        }.toTypedArray()
        it.drawableDomain = extension.drawables.toTypedArray()
        variant.registerGeneratedResFolders(project.files(drawablesOutputDirectory).builtBy(it))
    }

    private fun config(
        it: DimensionTask,
        dimensOutputDirectory: File,
        extension: SmlExtension,
        variant: BaseVariant,
        project: Project
    ) {
        it.group = "sml"
        it.outputFile = File(dimensOutputDirectory, "values/generated_dimens.xml")
        it.dimensMap = extension.dimen.get()
        variant.registerGeneratedResFolders(project.files(dimensOutputDirectory).builtBy(it))
    }

    private fun config(
        it: ColorTask,
        colorsOutputDirectory: File,
        extension: SmlExtension,
        variant: BaseVariant,
        project: Project
    ) {
        it.group = "sml"
        it.outputFile = File(colorsOutputDirectory, "values/generated_colors.xml")
        it.colorsMap = extension.color.get()
        variant.registerGeneratedResFolders(project.files(colorsOutputDirectory).builtBy(it))
    }

    private fun taskName(variant: BaseVariant, type: String) = "generate$type${variant.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }}"
}