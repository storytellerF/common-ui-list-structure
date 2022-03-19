package com.storyteller_f.common_ui

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.core.view.MarginLayoutParamsCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams

fun <T : View> T.setOnClick(block: (T) -> Unit) {
    setOnClickListener {
        block(this)
    }
}

fun WindowInsets.navigator() =
    WindowInsetsCompat.toWindowInsetsCompat(this)
        .getInsets(WindowInsetsCompat.Type.navigationBars())

fun WindowInsets.status() =
    WindowInsetsCompat.toWindowInsetsCompat(this)
        .getInsets(WindowInsetsCompat.Type.statusBars())

inline fun View.updateMargins(block: ViewGroup.MarginLayoutParams.() -> Unit) {
    updateLayoutParams(block)
}

fun View.updateMargin(rect: Direction) {
    updateMargins {
        marginStart = rect.start
        marginEnd = rect.end
        topMargin = rect.top
        bottomMargin = rect.bottom
    }
}