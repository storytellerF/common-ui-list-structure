package com.storyteller_f.common_ui

import android.view.View

fun <T : View> T.setOnClick(block: (T) -> Unit) {
    setOnClickListener {
        block(this)
    }
}