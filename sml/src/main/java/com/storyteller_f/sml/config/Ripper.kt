@file:Suppress("FunctionName", "unused")

package com.storyteller_f.sml.config

import com.storyteller_f.sml.tasks.DrawableDomain

class ClipDrawable(drawable: Drawables, gravity: String, orientation: String) : Drawable() {
    init {
        elements.appendLine(
            """
            <clip android:drawable="$drawable" android:gravity="$gravity" android:clipOrientation="$orientation"
                xmlns:android="http://schemas.android.com/apk/res/android" />
        """.trimIndent()
        )
    }

    override fun process() = ""

    override fun endTag() = "</clip>"
}

interface IItem {
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

class RipperDrawable(color: Color, radius: Dimension) : Drawable(), IItem by Item() {
    init {
        elements.appendLine(
            """
            <ripple android:color="#ff0000"
                android:radius="$radius"
                android:effectColor="$color"
                xmlns:android="http://schemas.android.com/apk/res/android">
        """.trimIndent()
        )
        indirectForItem(elements)
    }

    override fun process() = ""

    override fun endTag() = "</ripper>"
}

fun DrawableDomain.Clip(drawableReference: DrawableReference, gravity: String, orientation: String, block: ClipDrawable.() -> Unit) {
    drawable.set(ClipDrawable(drawableReference, gravity, orientation).apply(block).output())
}

fun DrawableDomain.Ripper(color: Color, radius: Dimension, block: RipperDrawable.() -> Unit) {
    drawable.set(RipperDrawable(color, radius).apply(block).output())
}