package com.storyteller_f.plugin_core

import android.content.ContentResolver
import android.net.Uri

object FileSystemProviderConstant {
    const val fileName = "file name"
    const val filePath = "file path"
    const val fileSize = "file size"
    const val fileMimeType = "file mime type"

    const val typeUnknown = 0
    const val typeInfo = 1
    const val typeList = 2
    const val typeSibling = 3
}

object FileSystemProviderResolver {
    fun resolve(uri: Uri): Pair<Int, String>? {
        val uriPath = uri.path ?: return null
        return uriPath.let {
            val startIndex = it.indexOf("/", 1)
            val t = when (it.substring(1, startIndex)) {
                "info" -> FileSystemProviderConstant.typeInfo
                "list" -> FileSystemProviderConstant.typeList
                "siblings" -> FileSystemProviderConstant.typeSibling
                else -> FileSystemProviderConstant.typeUnknown
            }
            t to it.substring(startIndex)
        }
    }

    fun build(authority: String, type: Int, path: String): Uri? {
        val appender = when (type) {
            FileSystemProviderConstant.typeInfo -> "info"
            FileSystemProviderConstant.typeList -> "list"
            FileSystemProviderConstant.typeSibling -> "siblings"
            else -> return null
        }
        return Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority).path("/$appender$path").build()
    }
}