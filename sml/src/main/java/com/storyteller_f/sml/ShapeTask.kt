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

class RichRect(val left: String, val top: String, val right: String, val bottom: String) {
    fun print(prefix: String = "") {
        val inset = StringBuilder()
        inset.appendLine("""${prefix}Left=$left""")
        inset.appendLine("""${prefix}Top=$top""")
        inset.appendLine("""${prefix}Right=$right""")
        inset.appendLine("""${prefix}Bottom=$bottom""")
    }
}

abstract class ShapeDrawable(
    private val shape: String,
    private val dither: Boolean = false,
    private val visible: Boolean = true,
    private val tint: String? = null,
    private val tintMode: String? = null,
    private val optionalInsetLeft: String? = null,
    private val optionalInsetTop: String? = null,
    private val optionalInsetRight: String? = null,
    private val optionalInsetBottom: String? = null
) : Drawable() {

    fun start() {
        val tintBuilder = StringBuilder()
        if (tint != null) {
            tintBuilder.appendLine("tint=\"$tint\"")
            if (tintMode != null)
                tintBuilder.appendLine("tintMode=\"$tintMode\"")
        }
        val inset = StringBuilder()
        if (optionalInsetLeft != null) inset.appendLine("optionalInsetLeft=\"$optionalInsetLeft\"")
        if (optionalInsetTop != null) inset.appendLine("optionalInsetTop=\"$optionalInsetTop\"")
        if (optionalInsetRight != null) inset.appendLine("optionalInsetRight=\"$optionalInsetRight\"")
        if (optionalInsetBottom != null) inset.appendLine("optionalInsetRight=\"$optionalInsetBottom\"")
        content.appendLine(
            """
<shape android:shape="$shape"
    dither="$dither"
    visible="$visible"
${extraParam().prependIndent()}
${tintBuilder.toString().prependIndent()}
${inset.toString().prependIndent()}
    xmlns:android="http://schemas.android.com/apk/res/android">
            """.trimIndent()
        )
    }

    open fun extraParam(): String = ""

    override fun output(): String {
        return super.output() + "</shape>"
    }
}

class RectangleShapeDrawable(
    dither: Boolean = false,
    visible: Boolean = true,
    tint: String? = null,
    tintMode: String? = null,
    optionalInsetLeft: String? = null,
    optionalInsetTop: String? = null,
    optionalInsetRight: String? = null,
    optionalInsetBottom: String? = null
) : ShapeDrawable("rectangle", dither, visible, tint, tintMode, optionalInsetLeft, optionalInsetTop, optionalInsetRight, optionalInsetBottom), IStroke by Stroke(), IAppearance by Appearance(),
    IRound by Round() {
    init {
        indirectForAppearance(content)
        indirectForStroke(content)
        indirectForRound(content)
    }
}

class OvalShapeDrawable(
    dither: Boolean = false,
    visible: Boolean = true,
    tint: String? = null,
    tintMode: String? = null,
    optionalInsetLeft: String? = null,
    optionalInsetTop: String? = null,
    optionalInsetRight: String? = null,
    optionalInsetBottom: String? = null
) : ShapeDrawable("oval", dither, visible, tint, tintMode, optionalInsetLeft, optionalInsetTop, optionalInsetRight, optionalInsetBottom), IAppearance by Appearance() {
    init {
        indirectForAppearance(content)
    }
}

class RingShapeDrawable(
    private val innerRadius: String,
    private val thickness: String,
    private val isRatio: Boolean,
    dither: Boolean = false,
    visible: Boolean = true,
    tint: String? = null,
    tintMode: String? = null,
    optionalInsetLeft: String? = null,
    optionalInsetTop: String? = null,
    optionalInsetRight: String? = null,
    optionalInsetBottom: String? = null
) : ShapeDrawable("ring", dither, visible, tint, tintMode, optionalInsetLeft, optionalInsetTop, optionalInsetRight, optionalInsetBottom), IStroke by Stroke() {

    init {
        indirectForStroke(content)
    }

    override fun extraParam(): String {
        return """
        android:innerRadius${isRatioExtra(isRatio)}="$innerRadius"
        android:thickness${isRatioExtra(isRatio)}="$thickness"""".trimIndent()
    }

    private fun isRatioExtra(isRatio: Boolean): String {
        return "Ratio".takeIf { isRatio } ?: ""
    }

    val ring get() = ::stroke
}

interface IStroke {
    fun stroke(color: String, width: String)
    fun indirectForStroke(stringBuilder: StringBuilder)
}

interface IRound {
    fun corners(radius: String)
    fun corners(leftTop: String, leftBottom: String, rightTop: String, rightBottom: String)
    fun indirectForRound(stringBuilder: StringBuilder)
}

interface IAppearance {
    fun solid(color: String)
    fun linearGradient(startColor: String, endColor: String, angle: Float = 0F, useLevel: String = "false")
    fun linearGradient(startColor: String, endColor: String, centerColor: String, centerX: Float = 0.5f, centerY: Float = 0.5f, angle: Float = 0F, useLevel: String = "false")
    fun radialGradient(startColor: String, endColor: String, centerColor: String, gradientRadius: String, useLevel: String = "false")
    fun sweepGradient(startColor: String, endColor: String, centerColor: String, useLevel: String = "false")
    fun padding(left: String, top: String, right: String, bottom: String)
    fun size(width: String, height: String)

    fun indirectForAppearance(stringBuilder: StringBuilder)
}

class Appearance : IAppearance {
    private var content: StringBuilder? = null
    override fun solid(color: String) {
        content?.appendLine("""<solid android:color="$color"/>""".prependIndent())
    }

    override fun linearGradient(startColor: String, endColor: String, angle: Float, useLevel: String) {
        content?.appendLine(
            """
            <gradient android:type="linear" 
                android:endColor="$endColor"
                android:startColor="$startColor"
                android:useLevel="$useLevel"
                android:angle="$angle"/>
        """.trimIndent().prependIndent()
        )
    }

    override fun linearGradient(startColor: String, endColor: String, centerColor: String, centerX: Float, centerY: Float, angle: Float, useLevel: String) {
        content?.appendLine(
            """
            <gradient android:type="linear" 
                android:endColor="$endColor"
                android:startColor="$startColor"
                android:useLevel="$useLevel"
                android:angle="$angle"
                android:centerColor="$centerColor"
                android:centerX="$centerX"
                android:centerY="$centerY"/>
        """.trimIndent().prependIndent()
        )
    }

    override fun radialGradient(startColor: String, endColor: String, centerColor: String, gradientRadius: String, useLevel: String) {
        content?.appendLine(
            """
            <gradient android:type="radial" 
                android:endColor="$endColor"
                android:startColor="$startColor"
                android:useLevel="$useLevel"
                android:centerColor="$centerColor"
                android:gradientRadius="$gradientRadius"/>
        """.trimIndent().prependIndent()
        )
    }

    override fun sweepGradient(startColor: String, endColor: String, centerColor: String, useLevel: String) {
        content?.appendLine(
            """
            <gradient android:type="sweep" 
                android:endColor="$endColor"
                android:startColor="$startColor"
                android:useLevel="$useLevel"
                android:centerColor="$centerColor"/>
        """.trimIndent().prependIndent()
        )
    }


    override fun padding(left: String, top: String, right: String, bottom: String) {
        content?.appendLine(
            """<padding android:top="$top" android:right="$right" android:left="$left" android:bottom="$bottom"/>""".prependIndent()
        )
    }

    override fun size(width: String, height: String) {
        content?.appendLine("""<size android:width="$width" android:height="$height"/>""".prependIndent())
    }

    override fun indirectForAppearance(stringBuilder: StringBuilder) {
        content = stringBuilder
    }

}

class Stroke : IStroke {
    private var content: StringBuilder? = null
    override fun stroke(color: String, width: String) {
        content?.appendLine(
            """<stroke android:color="$color" android:width="$width"/>""".prependIndent()
        )
    }

    override fun indirectForStroke(stringBuilder: StringBuilder) {
        content = stringBuilder
    }
}

class Round : IRound {
    private var content: StringBuilder? = null

    override fun corners(radius: String) {
        content?.appendLine("""<corners android:radius="$radius"/>""".prependIndent())
    }

    override fun corners(leftTop: String, leftBottom: String, rightTop: String, rightBottom: String) {
        content?.appendLine(
            """<corners android:bottomLeftRadius="$leftBottom" android:topRightRadius="$rightTop" 
                android:bottomRightRadius="$rightBottom" android:topLeftRadius="$leftTop"/>""".prependIndent()
        )
    }

    override fun indirectForRound(stringBuilder: StringBuilder) {
        content = stringBuilder
    }

}

class LineShapeDrawable(
    dither: Boolean = false,
    visible: Boolean = true,
    tint: String? = null,
    tintMode: String? = null,
    optionalInsetLeft: String? = null,
    optionalInsetTop: String? = null,
    optionalInsetRight: String? = null,
    optionalInsetBottom: String? = null
) : ShapeDrawable("line", dither, visible, tint, tintMode, optionalInsetLeft, optionalInsetTop, optionalInsetRight, optionalInsetBottom), IStroke by Stroke() {
    init {
        indirectForStroke(content)
    }

    val line get() = ::stroke
}

fun DrawableDomain.Rectangle(block: RectangleShapeDrawable.() -> Unit) {
    drawable.set(
        RectangleShapeDrawable().apply { start() }.apply(block).output()
    )
}

fun DrawableDomain.Oval(block: OvalShapeDrawable.() -> Unit) {
    drawable.set(OvalShapeDrawable().apply {
        start()
    }.apply(block).output())
}

fun DrawableDomain.Ring(innerRadius: String, thickness: String, block: RingShapeDrawable.() -> Unit) {
    drawable.set(RingShapeDrawable(innerRadius, thickness, false).apply { start() }.apply(block).output())
}

fun DrawableDomain.Ring(innerRadiusRatio: Float, thicknessRatio: Float, block: RingShapeDrawable.() -> Unit) {
    drawable.set(RingShapeDrawable(innerRadiusRatio.toString(), thicknessRatio.toString(), true).apply { start() }.apply(block).output())
}

fun DrawableDomain.Line(block: LineShapeDrawable.() -> Unit) {
    drawable.set(LineShapeDrawable().apply { start() }.apply(block).output())
}


interface DrawableDomain {
    // Type must have a read-only 'name' property
    val name: String?

    val drawable: Property<String>
}