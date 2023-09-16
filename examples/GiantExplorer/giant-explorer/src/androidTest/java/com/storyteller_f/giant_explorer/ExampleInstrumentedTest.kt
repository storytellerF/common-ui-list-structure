package com.storyteller_f.giant_explorer

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.storyteller_f.file_system.instance.local.DocumentLocalFileInstance
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.storyteller_f.giant_explorer", appContext.packageName)
    }

    @Test
    fun test() {
        val parse = Uri.parse("ftp://hdh:hello@localhost:2121/")
        assert(parse.authority == "hdh:hello@localhost:2121")
    }

    @Test
    fun testPing() {
        val uriFromAuthority = DocumentLocalFileInstance.uriFromAuthority(
            DocumentLocalFileInstance.EXTERNAL_STORAGE_DOCUMENTS,
            "/"
        )
        uriFromAuthority.path
    }
}