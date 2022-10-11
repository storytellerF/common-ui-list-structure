package com.storyteller_f.sml.tasks

import com.storyteller_f.sml.writeXlmWithTags
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

internal open class ColorTask : DefaultTask() {
    @get:OutputFile
    lateinit var outputFile: File

    @get:Input
    lateinit var colorsMap: MutableMap<String, String>
    @TaskAction
    fun makeResources() {
        colorsMap.entries.joinToString { (colorName, color) ->
            "\n    <color name=\"$colorName\">$color</color>"
        }.also { xml ->
            outputFile.writeXlmWithTags(xml)
        }
    }
}