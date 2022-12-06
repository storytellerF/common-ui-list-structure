package com.storyteller_f.giant_explorer.service

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.storyteller_f.giant_explorer.BuildConfig
import com.storyteller_f.giant_explorer.control.getFileInstance
import java.io.File


class FileSystemProvider : ContentProvider() {
    private val matcher = UriMatcher(UriMatcher.NO_MATCH)

    init {
        matcher.addURI(BuildConfig.FILE_SYSTEM_PROVIDER_AUTHORITY, "/info/*", 1)
        matcher.addURI(BuildConfig.FILE_SYSTEM_PROVIDER_AUTHORITY, "/list/*", 2)
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val encodedPath = uri.encodedPath ?: return null
        val filePath = encodedPath.substring(5)
        return ParcelFileDescriptor.open(File(filePath), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        val encodedPath = uri.encodedPath ?: return null
        val c = context ?: return null
        return when (matcher.match(uri)) {
            1 -> {
                val filePath = encodedPath.substring(5)
                val fileInstance = getFileInstance(filePath, c)
                MatrixCursor(fileProjection).apply {
                    addRow(arrayOf(fileInstance.file.name, fileInstance.file.name, fileInstance.fileLength))
                }
            }
            2 -> {
                null
            }
            else -> null
        }
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        TODO("Not yet implemented")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        TODO("Not yet implemented")
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        TODO("Not yet implemented")
    }

    companion object {
        const val file_name = "file name"
        const val file_path = "file path"
        const val file_size = "file size"
        private val fileProjection = arrayOf(
            file_name, file_path, file_size
        )
    }
}