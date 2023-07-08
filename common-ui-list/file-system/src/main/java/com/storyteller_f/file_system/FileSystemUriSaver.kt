package com.storyteller_f.file_system

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

class FileSystemUriSaver {
    private val documentRootCache = HashMap<String, Uri>(2)
    fun savedUri(context: Context, sharedPreferenceKey: String): Uri? {
        if (!documentRootCache.containsKey(sharedPreferenceKey)) {
            val sharedPreferences = getSharedPreferences(context)
            val uriString = sharedPreferences.getString(sharedPreferenceKey, null) ?: return null
            val root = Uri.parse(uriString)
            documentRootCache[sharedPreferenceKey] = root
        }
        return documentRootCache[sharedPreferenceKey]
    }

    fun saveUri(context: Context, key: String, uri: Uri) {
        val sharedPreferences = getSharedPreferences(context)
        sharedPreferences.edit().putString(key, uri.toString()).apply()
        documentRootCache[key] = uri
    }

    fun savedUris(context: Context): List<String> {
        if (documentRootCache.isEmpty()) {
            val sharedPreferences = getSharedPreferences(context)
            for (k in sharedPreferences.all.keys) {
                val uriString = sharedPreferences.getString(k, null)
                if (uriString != null) documentRootCache[k] = Uri.parse(uriString)
            }
        }
        return ArrayList(documentRootCache.keys)
    }

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences("file-system-uri", Context.MODE_PRIVATE)
    }

    companion object {
        val instance = FileSystemUriSaver()
    }
}
