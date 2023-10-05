package com.storyteller_f.giant_explorer.control.plugin

import android.content.ContentResolver
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileSystemProviderResolverTest {

    @Test
    fun build() {
        val build =
            Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority("com.hello").path("test")
                .build()
        val build1 = FileSystemProviderResolver.share(false, build)
        val resolvePath = FileSystemProviderResolver.resolve(build1!!)
        assertEquals(build.toString(), resolvePath.toString())
    }
}