package com.storyteller_f.sml

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.Serializable
import java.nio.file.Files

internal open class ShapeTask : DefaultTask() {
    @get:Input
    lateinit var outputDirectory: String

    @get:OutputFiles
    lateinit var outputFile: Array<File>

    @get:Input
    lateinit var drawableDomain: Array<DrawableDomain>

    @TaskAction
    fun makeResources() {
        drawableDomain.forEach {
            val content = it
            val file = File(outputDirectory, "${it.name}.xml")
            if (file.createNewFile()) {
                Files.writeString(file.toPath(), content.drawable.get().output())
            } else {
                println("sml：create file${file.absolutePath} failure")
            }
        }

    }
}

abstract class Drawable : Serializable {
    val content: StringBuilder = StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
    open fun output() = content.toString()
}

abstract class ShapeDrawable : Drawable() {
    override fun output(): String {
        return super.output() + "\n</shape>"
    }
}

class RectangleShapeDrawable : ShapeDrawable() {
    init {
        content.append(
            """
<shape android:shape="rectangle"
    xmlns:android="http://schemas.android.com/apk/res/android">""".trim()
        )
    }

    fun solid(color: String) {
        content.append("<solid android:color=\"$color\"/>\n")
    }

    fun corners(radius: String) {
        content.append("<corners android:radius=\"$radius\"/>\n")
    }
}

class OvalShapeDrawable : ShapeDrawable() {
    init {
        content.append(
            """
<shape android:shape="oval"
    xmlns:android="http://schemas.android.com/apk/res/android">""".trim()
        )
    }
    fun solid(color: String) {
        content.append("<solid android:color=\"$color\"/>\n")
    }
}

fun DrawableDomain.Rectangle(block: RectangleShapeDrawable.() -> Unit) {
    drawable.set(RectangleShapeDrawable().apply(block))
}

fun DrawableDomain.Oval(block: OvalShapeDrawable.() -> Unit) {
    drawable.set(OvalShapeDrawable().apply(block))
}


interface DrawableDomain {
    // Type must have a read-only 'name' property
    val name: String?

    val drawable: Property<Drawable>
}