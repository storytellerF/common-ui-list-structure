@file:Suppress("FunctionName", "unused")

package com.storyteller_f.sml.config

import com.storyteller_f.sml.tasks.DrawableDomain

class RectangleShapeDrawable : ShapeDrawable("rectangle"), IStroke by Stroke(), IAppearance by Appearance(),
    IRound by Round() {
    init {
        indirectForAppearance(elements)
        indirectForStroke(elements)
        indirectForRound(elements)
    }
}

class OvalShapeDrawable : ShapeDrawable("oval"), IAppearance by Appearance() {
    init {
        indirectForAppearance(elements)
    }
}

class RingShapeDrawable(
    private val innerRadius: String,
    private val thickness: String,
    private val isRatio: Boolean,
) : ShapeDrawable("ring"), IStroke by Stroke() {

    init {
        indirectForStroke(elements)
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
    fun stroke(color: Color, width: Dimension)
    fun indirectForStroke(stringBuilder: StringBuilder)
}

interface IRound {
    fun corners(radius: Dimension)
    fun corners(leftTop: Dimension, leftBottom: Dimension, rightTop: Dimension, rightBottom: Dimension)
    fun indirectForRound(stringBuilder: StringBuilder)
}

interface IAppearance {
    fun solid(color: Color)
    fun linearGradient(startColor: Color, endColor: Color, angle: Float = 0F, useLevel: String = "false")
    fun linearGradient(startColor: Color, endColor: Color, centerColor: Color, centerX: Float = 0.5f, centerY: Float = 0.5f, angle: Float = 0F, useLevel: String = "false")
    fun radialGradient(startColor: Color, endColor: Color, centerColor: Color, gradientRadius: String, useLevel: String = "false")
    fun sweepGradient(startColor: Color, endColor: Color, centerColor: Color, useLevel: String = "false")
    fun padding(left: Dimension, top: Dimension, right: Dimension, bottom: Dimension)
    fun size(width: Dimension, height: Dimension)

    fun indirectForAppearance(stringBuilder: StringBuilder)
}

class Appearance : IAppearance {
    private var content: StringBuilder? = null
    override fun solid(color: Color) {
        content?.appendLine("""<solid android:color="$color"/>""".prependIndent())
    }

    override fun linearGradient(startColor: Color, endColor: Color, angle: Float, useLevel: String) {
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

    override fun linearGradient(startColor: Color, endColor: Color, centerColor: Color, centerX: Float, centerY: Float, angle: Float, useLevel: String) {
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

    override fun radialGradient(startColor: Color, endColor: Color, centerColor: Color, gradientRadius: String, useLevel: String) {
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

    override fun sweepGradient(startColor: Color, endColor: Color, centerColor: Color, useLevel: String) {
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


    override fun padding(left: Dimension, top: Dimension, right: Dimension, bottom: Dimension) {
        content?.appendLine(
            """<padding android:top="$top" android:right="$right" android:left="$left" android:bottom="$bottom"/>""".prependIndent()
        )
    }

    override fun size(width: Dimension, height: Dimension) {
        content?.appendLine("""<size android:width="$width" android:height="$height"/>""".prependIndent())
    }

    override fun indirectForAppearance(stringBuilder: StringBuilder) {
        content = stringBuilder
    }

}

class Stroke : IStroke {
    private var content: StringBuilder? = null
    override fun stroke(color: Color, width: Dimension) {
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

    override fun corners(radius: Dimension) {
        content?.appendLine("""<corners android:radius="$radius"/>""".prependIndent())
    }

    override fun corners(leftTop: Dimension, leftBottom: Dimension, rightTop: Dimension, rightBottom: Dimension) {
        content?.appendLine(
            """<corners android:bottomLeftRadius="$leftBottom" android:topRightRadius="$rightTop" 
                android:bottomRightRadius="$rightBottom" android:topLeftRadius="$leftTop"/>""".prependIndent()
        )
    }

    override fun indirectForRound(stringBuilder: StringBuilder) {
        content = stringBuilder
    }

}

class LineShapeDrawable : ShapeDrawable("line"), IStroke by Stroke() {
    init {
        indirectForStroke(elements)
    }

    val line get() = ::stroke
}

abstract class ShapeDrawable(private val shape: String) : Drawable() {
    val dither: Boolean = false
    val visible: Boolean = true
    val tint: Tint? = null
    val optionalInset: OptionalInset? = null

    override fun process(): String {
        val tintBuilder = StringBuilder()
        if (tint?.tint != null) {
            tintBuilder.appendLine("tint=\"${tint.tint}\"")
            if (tint.tintMode != null)
                tintBuilder.appendLine("tintMode=\"${tint.tintMode}\"")
        }
        val inset = StringBuilder()
        if (optionalInset?.left != null) inset.appendLine("optionalInsetLeft=\"${optionalInset.left}\"")
        if (optionalInset?.top != null) inset.appendLine("optionalInsetTop=\"${optionalInset.top}\"")
        if (optionalInset?.right != null) inset.appendLine("optionalInsetRight=\"${optionalInset.right}\"")
        if (optionalInset?.bottom != null) inset.appendLine("optionalInsetRight=\"${optionalInset.bottom}\"")
        return """
<shape android:shape="$shape"
    dither="$dither"
    visible="$visible"
${extraParam().prependIndent()}
${tintBuilder.toString().prependIndent()}
${inset.toString().prependIndent()}
    xmlns:android="http://schemas.android.com/apk/res/android">
            """.trimIndent()
    }

    open fun extraParam(): String = ""

    override fun endTag(): String {
        return "</shape>"
    }
}

fun DrawableDomain.Rectangle(block: RectangleShapeDrawable.() -> Unit) {
    drawable.set(
        RectangleShapeDrawable().apply(block).output()
    )
}

fun DrawableDomain.Oval(block: OvalShapeDrawable.() -> Unit) {
    drawable.set(OvalShapeDrawable().apply(block).output())
}

fun DrawableDomain.Ring(innerRadius: String, thickness: String, block: RingShapeDrawable.() -> Unit) {
    drawable.set(RingShapeDrawable(innerRadius, thickness, false).apply(block).output())
}

fun DrawableDomain.Ring(innerRadiusRatio: Float, thicknessRatio: Float, block: RingShapeDrawable.() -> Unit) {
    drawable.set(RingShapeDrawable(innerRadiusRatio.toString(), thicknessRatio.toString(), true).apply(block).output())
}

fun DrawableDomain.Line(block: LineShapeDrawable.() -> Unit) {
    drawable.set(LineShapeDrawable().apply(block).output())
}

