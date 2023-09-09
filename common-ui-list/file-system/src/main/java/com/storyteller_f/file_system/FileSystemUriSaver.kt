package com.storyteller_f.file_system

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import com.google.gson.Gson
import java.io.File

class SavedUris(val uris: MutableMap<String, MutableMap<String, String>>)

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class FileSystemUriSaver {
    /**
     * key authority
     * value
     *  key root
     *  value root uri
     */
    private lateinit var saved: SavedUris
    private val uris by lazy {
        saved.uris
    }
    private val gson by lazy {
        Gson()
    }
    private val Context.file
        get() = File(filesDir, "uri.saved").apply {
            if (!exists()) {
                createNewFile()
            }
        }

    private fun restoreCheck(context: Context) {
        if (::saved.isInitialized) return

        val readText = context.file.readText()
        saved = if (readText.isEmpty()) {
            SavedUris(mutableMapOf())
        } else {
            gson.fromJson(
                readText,
                SavedUris::class.java
            )
        }
    }

    fun savedUri(context: Context, sharedPreferenceKey: String): Uri? {
        restoreCheck(context)
        return uris[sharedPreferenceKey]?.values?.first()?.toUri()
    }

    fun saveUri(context: Context, key: String, uri: Uri) {
        restoreCheck(context)
        val rootId = DocumentsContract.getTreeDocumentId(uri)
        uris[key] = mutableMapOf(rootId to uri.toString())
        val toJson = gson.toJson(saved)
        context.file.writeText(toJson)
    }

    fun savedUris(context: Context): Map<String, String> {
        restoreCheck(context)
        val map = mutableMapOf<String, String>()
        uris.keys.forEach {
            map[it] = uris[it]?.keys?.firstOrNull()!!
        }
        return map
    }

    companion object {
        val instance = FileSystemUriSaver()
    }
}
