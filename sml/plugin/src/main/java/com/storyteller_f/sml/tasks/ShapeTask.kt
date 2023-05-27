package com.storyteller_f.sml.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files

internal open class ShapeTask : DefaultTask() {
    @get:OutputDirectory
    lateinit var outputDirectory: File

    @get:OutputFiles
    lateinit var outputFileList: Array<File>

    @get:Input
    lateinit var drawableDomains: Array<DrawableDomain>

    @TaskAction
    fun makeResources() {
        drawableDomains.forEach {
            val content = it
            val file = File(outputDirectory, "${it.name}.xml")
            Files.writeString(file.toPath(), content.drawable.get())
        }

    }
}

interface DrawableDomain {
    // Type must have a read-only 'name' property
    val name: String?

    val drawable: Property<String>
}