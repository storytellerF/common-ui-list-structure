package com.storyteller_f.sml

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files

internal open class ShapeTask : DefaultTask() {
    @get:Input
    lateinit var outputDirectory: String

    @get:OutputFile
    lateinit var outputFile: Array<File>

    @get:Input
    lateinit var shapeDomain: Array<ShapeDomain>

    @TaskAction
    fun makeResources() {
        shapeDomain.forEach {
            val content = """
<?xml version="1.0" encoding="utf-8"?>
<shape android:shape="rectangle"
    xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="${it.solidColor.get()}"/>
    <corners android:radius="${it.radius.get()}dp"/>
</shape>""".trim()
            Files.writeString(File(outputDirectory, "${it.name}.xml").toPath(), content)
        }

    }
}