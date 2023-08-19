package com.storyteller_f.file_system

import android.os.Build
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File


@RunWith(AndroidJUnit4::class)
class TestRequestPermission {

    @get:Rule
    var mActivityRule = ActivityScenarioRule(
        AndroidTestActivity::class.java
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testRequestPermission() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            val uri = File(FileInstanceFactory.rootUserEmulatedPath).toUri()

            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            mActivityRule.scenario.onActivity {
                it.lifecycleScope.launch {
                    it.requestPathPermission(uri)
                }
            }

            val instance = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            instance.findObject(UiSelector().text("去授予")).click()
            instance.findObject(UiSelector().textContains("ALLOW")).click()
            instance.findObject(UiSelector().text("ALLOW")).click()
            runTest {
                assertTrue(appContext.checkPathPermission(uri))
            }
        }

    }
}