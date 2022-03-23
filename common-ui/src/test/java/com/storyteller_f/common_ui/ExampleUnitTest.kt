package com.storyteller_f.common_ui

import android.graphics.Rect
import androidx.core.graphics.plus
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val plus = Rect(1, 1, 1, 1).plus(Rect(2, 0, 2, 0))
        assertEquals(plus.left, 3)
    }
}