package com.storyteller_f.sml

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
    lateinit var outputFile: Array<File>

    @get:Input
    lateinit var drawableDomain: Array<DrawableDomain>

    @TaskAction
    fun makeResources() {
        drawableDomain.forEach {
            val content = it
            val file = File(outputDirectory, "${it.name}.xml")
            Files.writeString(file.toPath(), content.drawable.get())
        }

    }
}

open class Drawable {
    val content: StringBuilder = StringBuilder(
        """
            <?xml version="1.0" encoding="utf-8"?>
        """.trimIndent()
    ).apply { appendLine() }

    open fun output() = content.toString()
}

class ClipDrawable(drawable: String, gravity: String, orientation: String) : Drawable() {
    init {
        content.appendLine(
            """
            <clip android:drawable="@drawable/ic_eye" android:gravity="$gravity" android:clipOrientation="$orientation"
                xmlns:android="http://schemas.android.com/apk/res/android" />
        """.trimIndent()
        )
    }
}

interface IItem{
    /**
     * start top end bottom 是padding
     */
    fun addItem(start: String, top: String, end: String, bottom: String, width: String, height: String, drawable: String, gravity: String, id: String)

    fun indirectForItem(stringBuilder: java.lang.StringBuilder)
}

class Item : IItem {
    private var content: StringBuilder? = null

    override fun addItem(start: String, top: String, end: String, bottom: String, width: String, height: String, drawable: String, gravity: String, id: String) {
        content?.appendLine(
            """
                <item android:start="$start" top="$top" end="$end" bottom="$bottom" width="$width" height="$height" drawable="$drawable" gravity="$gravity" id="$id"/>
            """.trimIndent().prependIndent()
        )
    }

    override fun indirectForItem(stringBuilder: StringBuilder) {
        content = stringBuilder
    }

}

class RipperDrawable(color: String, radius: String) : Drawable(), IItem by Item() {
    init {
        content.appendLine("""
            <ripple android:color="#ff0000"
                android:radius="12dp"
                android:effectColor="@color/white"
                xmlns:android="http://schemas.android.com/apk/res/android">
        """.trimIndent())
        indirectForItem(content)
    }

    override fun output() = super.output() + "</ripple>"
}

fun DrawableDomain.Clip(drawableReference: String, gravity: String, orientation: String, block: ClipDrawable.() -> Unit) {
    drawable.set(ClipDrawable(drawableReference, gravity, orientation).apply(block).output())
}

fun DrawableDomain.Ripper(color: String, radius: String, block: RipperDrawable.() -> Unit) {
    drawable.set(RipperDrawable(color, radius).apply(block).output())
}

interface DrawableDomain {
    // Type must have a read-only 'name' property
    val name: String?

    val drawable: Property<String>
}