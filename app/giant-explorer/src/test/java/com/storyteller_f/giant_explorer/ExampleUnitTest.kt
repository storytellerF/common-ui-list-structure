package com.storyteller_f.giant_explorer

import com.storyteller_f.file_system.FileInstanceFactory
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        val simplyPath = FileInstanceFactory.simplyPath(FileInstanceFactory.rootUserEmulatedPath)
        assert(simplyPath == FileInstanceFactory.rootUserEmulatedPath)
    }
}