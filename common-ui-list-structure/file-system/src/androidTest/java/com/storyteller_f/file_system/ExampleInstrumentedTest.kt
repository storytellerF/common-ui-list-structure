package com.storyteller_f.file_system

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.storyteller_f.file_system.instance.local.RegularLocalFileInstance
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
        //assertEquals("com.storyteller_f.file_system.test", appContext.packageName)
        val fileInstance = FileInstanceFactory.getFileInstance("/bin", appContext)
        val listSafe = fileInstance.listSafe()
//        val listFiles = File("/data/app").listFiles()
//        val installedApplications = appContext.packageManager.getInstalledApplications(
//            0
//        )
//        println(fileInstance.javaClass)
//        assertEquals(listSafe.count, 0)
    }
}