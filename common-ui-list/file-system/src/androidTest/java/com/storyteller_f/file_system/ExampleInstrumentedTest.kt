package com.storyteller_f.file_system

import androidx.core.net.toUri
import androidx.test.espresso.Espresso.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun testPrefix() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        listOf(
            FileInstanceFactory.currentEmulatedPath to FileInstanceFactory.currentEmulatedPath,
            "/storage/self/primary" to FileInstanceFactory.currentEmulatedPath,
            FileInstanceFactory.rootUserEmulatedPath to FileInstanceFactory.rootUserEmulatedPath,
            "/storage/XX44-XX55/Downloads" to "/storage/XX44-XX55",
            "/storage/XX44-XX55" to "/storage/XX44-XX55",
            FileInstanceFactory.storagePath to "fake"
        ).forEach {
            val prefix = FileInstanceFactory.getPrefix(appContext, File(it.first).toUri())
            assertEquals(it.second, prefix?.key)
        }
    }

    @Test
    fun testList() {
        runBlocking {
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            listOf(
                "/storage/self" to listOf("primary"),
                "/storage/self/primary" to listOf(),
            ).forEach { (it, expected) ->
                val fileInstance = FileInstanceFactory.getFileInstance(
                    appContext,
                    File(it).toUri(),
                )
                assertEquals(expected.size, fileInstance.list().count)
            }
        }

    }

}