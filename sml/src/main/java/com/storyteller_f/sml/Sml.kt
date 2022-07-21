@file:Suppress("unused")

package com.storyteller_f.sml

import com.android.build.gradle.api.BaseVariant
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import java.io.File
import java.util.*

interface SmlExtension {
    val color: MapProperty<String, String>
    val dimen: MapProperty<String, String>
    val shapes: NamedDomainObjectContainer<ShapeDomain>
}

interface ShapeDomain {
    // Type must have a read-only 'name' property
    val name: String?

    val solidColor: Property<String>
    val radius: Property<Int>
}

class Sml : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("sml", SmlExtension::class.java)
        val outputPath = "${project.buildDir}/generated/res"
        project.android().variants().all { variant ->
            val outputDirectory =
                File("$outputPath/${variant.dirName}").apply { mkdir() }
            project.tasks.register(taskName(variant, "Colors"), ColorTask::class.java) {
                it.group = "sml"
                it.outputFile = File(outputDirectory, "values/generated_colors.xml")
                it.colorsMap = extension.color.get()
                variant.registerGeneratedResFolders(project.files(outputDirectory).builtBy(it))
            }
            project.tasks.register(taskName(variant, "Dimens"), DimensionTask::class.java) {
                it.group = "sml"
                it.outputFile = File(outputDirectory, "values/generated_dimens.xml")
                it.dimensMap = extension.dimen.get()
                variant.registerGeneratedResFolders(project.files(outputDirectory).builtBy(it))
            }
            project.tasks.register(taskName(variant, "Shapes"), ShapeTask::class.java) { it ->
                it.group = "sml"
                val path = File(outputDirectory, "drawable")
                it.outputDirectory = path.absolutePath
                it.outputFile = extension.shapes.map { shapeDomain ->
                    File(path, "${shapeDomain.name}.xml")
                }.toTypedArray()
                it.shapeDomain = extension.shapes.toTypedArray()
                variant.registerGeneratedResFolders(project.files(outputDirectory).builtBy(it))
            }

        }


    }

    private fun taskName(variant: BaseVariant, type: String) = "generate$type${variant.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }}"
}