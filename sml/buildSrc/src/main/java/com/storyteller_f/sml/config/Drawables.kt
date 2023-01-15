@file:Suppress("unused")

package com.storyteller_f.sml.config


interface Dimension

class Dp(private val value: Float) : Dimension {
    override fun toString() = "${value}dp"
}

class Px(private val value: Int) : Dimension {
    override fun toString() = "${value}px"
}

class Sp(private val value: Float) : Dimension {
    override fun toString() = "${value}sp"
}

class In(private val value: Int) : Dimension {
    override fun toString() = "${value}in"
}

class Mm(private val value: Int) : Dimension {
    override fun toString() = "${value}mm"
}

class Pt(private val value: Int) : Dimension {
    override fun toString() = "${value}pt"
}

abstract class Reference(open val referenceName: String) {
    override fun toString() = "@${type()}/$referenceName"
    abstract fun type(): String
}

class DimensionReference(override val referenceName: String) : Reference(referenceName), Dimension {
    override fun type() = "dimen"
}

interface Drawables

class DrawableReference(override val referenceName: String) : Reference(referenceName), Drawables {
    override fun type() = "drawable"
}

interface Color

class RgbColor(private val color: String) : Color {
    override fun toString() = color
}

class ColorReference(override val referenceName: String) : Reference(referenceName), Color {
    override fun type() = "color"
}

class OptionalInset(val left: Dimension?, val top: Dimension?, val right: Dimension?, val bottom: Dimension?)

class Tint(val tint: String? = null, val tintMode: String? = null)


abstract class Drawable {
    val elements: StringBuilder = StringBuilder()

    fun output() =
        """<?xml version="1.0" encoding="utf-8"?>
            
        """.trimIndent() + process() + elements.toString() + endTag()

    abstract fun process(): String

    abstract fun endTag(): String
}