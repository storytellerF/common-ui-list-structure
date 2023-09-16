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

    fun savedUri(context: Context, sharedPreferenceKey: String, tree: String): Uri? {
        restoreCheck(context)
        return uris[sharedPreferenceKey]?.get(tree)?.toUri()
    }

    fun saveUri(context: Context, authority: String, uri: Uri, tree: String?) {
        restoreCheck(context)
        val rootId = tree ?: DocumentsContract.getTreeDocumentId(uri)
        uris.getOrPut(authority) {
            mutableMapOf()
        }.run {
            put(rootId, uri.toString())
        }
        val toJson = gson.toJson(saved)
        context.file.writeText(toJson)
    }

    fun savedUris(context: Context): Map<String, List<String>> {
        restoreCheck(context)
        val map = mutableMapOf<String, List<String>>()
        uris.keys.forEach {
            val treeList = uris[it]?.keys?.toList().orEmpty()
            if (treeList.isNotEmpty()) {
                map[it] = treeList
            }
        }
        return map
    }

    companion object {
        val instance = FileSystemUriSaver()
    }
}
