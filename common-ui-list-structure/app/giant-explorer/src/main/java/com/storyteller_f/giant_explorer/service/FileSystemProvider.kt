package com.storyteller_f.giant_explorer.service

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.giant_explorer.control.getFileInstance
import java.io.File


class FileSystemProvider : ContentProvider() {

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
        val (type, filePath) = encodedPath.let {
            val startIndex = it.indexOf("/", 1)
            it.substring(1, startIndex) to it.substring(startIndex)
        }
        Log.i(TAG, "query: file path:$filePath $type $uri")
        val fileInstance = getFileInstance(filePath, c)
        return when (type) {
            "info" -> {
                MatrixCursor(fileProjection).apply {
                    val file = fileInstance.file
                    addRow(arrayOf(file.name, file.fullPath, fileInstance.fileLength))
                }
            }
            "list" -> {
                require(fileInstance.isDirectory)
                queryFileInstance(fileInstance)
            }
            "siblings" -> {
                val toParent = fileInstance.toParent()
                queryFileInstance(toParent)
            }
            else -> null
        }
    }

    private fun queryFileInstance(fileInstance: FileInstance): MatrixCursor {
        val listSafe = fileInstance.listSafe()
        val matrixCursor = MatrixCursor(fileProjection)
        listSafe.directories.forEach {
            matrixCursor.addRow(arrayOf(it.name, it.fullPath, 0))
        }
        listSafe.files.forEach {
            matrixCursor.addRow(arrayOf(it.name, it.fullPath, it.size))
        }
        return matrixCursor
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
        private const val TAG = "FileSystemProvider"
        const val file_name = "file name"
        const val file_path = "file path"
        const val file_size = "file size"
        private val fileProjection = arrayOf(
            file_name, file_path, file_size
        )
    }
}