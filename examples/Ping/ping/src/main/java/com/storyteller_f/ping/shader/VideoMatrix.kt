package com.storyteller_f.ping.shader

data class VideoMatrix(val width: Int, val height: Int, val rotation: Int) {
    private val horizontalFlip: Boolean
        get() = rotation % 180 != 0

    val realHeight = if (horizontalFlip) width else height
    val realWidth = if (horizontalFlip) height else width
}