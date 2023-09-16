package com.storyteller_f.ping

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

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
        assertEquals("com.storyteller_f.ping", appContext.packageName)
        val root = appContext.root!!
        assertEquals(root.absolutePath, getDocument(root, null).absolutePath)
        assertEquals(
            root.absolutePath,
            getDocument(root, StorageProvider.ELEMENT_ID).absolutePath
        )
        assertEquals("${StorageProvider.ELEMENT_ID}/test", subDocumentId(File(root, "test"), root))
        assertEquals(StorageProvider.ELEMENT_ID, subDocumentId(root, root))
    }
}