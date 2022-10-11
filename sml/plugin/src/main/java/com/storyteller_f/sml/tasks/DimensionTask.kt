package com.storyteller_f.sml.tasks

import com.storyteller_f.sml.writeXlmWithTags
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

internal open class DimensionTask : DefaultTask() {
    @get:OutputFile
    lateinit var outputFile: File

    @get:Input
    lateinit var dimensMap: MutableMap<String, String>

    @TaskAction
    fun makeResources() {
        dimensMap.entries.joinToString { (dimensionName, value) ->
            "\n    <dimen name=\"$dimensionName\">$value</dimen>"
        }.also { xml ->
            outputFile.writeXlmWithTags(xml)
        }
    }
}