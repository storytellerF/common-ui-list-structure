package com.storyteller_f.file_system

import android.content.pm.PackageManager
import androidx.core.content.PackageManagerCompat
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.storyteller_f.file_system.util.FileUtility

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
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
        val fileInstance = FileInstanceFactory.getFileInstance("/system", appContext)
        val listSafe = fileInstance.listSafe()
        val listFiles = File("/").listFiles()
        appContext.packageManager.getInstalledApplications(
            PackageManager.MATCH_APEX
        ).map {
            it.publicSourceDir
        }
        assertEquals(listFiles.size, 0)
    }
}