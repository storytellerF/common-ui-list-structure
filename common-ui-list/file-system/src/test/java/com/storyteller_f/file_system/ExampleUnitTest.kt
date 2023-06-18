package com.storyteller_f.file_system

import com.storyteller_f.multi_core.StoppableTask
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testSimplePath() {
        val simplyPath = FileInstanceFactory.simplyPath(FileInstanceFactory.rootUserEmulatedPath, StoppableTask.Blocking)
        assert(simplyPath == FileInstanceFactory.rootUserEmulatedPath)
    }
}