package com.storyteller_f.giant_explorer.service

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.util.Log
import android.webkit.MimeTypeMap
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.giant_explorer.control.getFileInstance
import com.storyteller_f.plugin_core.FileSystemProviderConstant
import com.storyteller_f.plugin_core.FileSystemProviderResolver
import java.io.File


class FileSystemProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        return true
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val (_, path) = FileSystemProviderResolver.resolve(uri) ?: return null
        return ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        val c = context ?: return null
        val (type, filePath) = FileSystemProviderResolver.resolve(uri) ?: return null
        Log.i(TAG, "query: file path:$filePath $type $uri")
        val fileInstance = getFileInstance(filePath, c)
        return when (type) {
            FileSystemProviderConstant.typeInfo -> {
                MatrixCursor(fileProjection).apply {
                    val file = fileInstance.file
                    addRow(arrayOf(file.name, file.fullPath, fileInstance.fileLength))
                }
            }
            FileSystemProviderConstant.typeList -> {
                require(fileInstance.isDirectory)
                queryFileInstance(fileInstance)
            }
            FileSystemProviderConstant.typeSibling -> {
                val toParent = fileInstance.toParent()
                queryFileInstance(toParent)
            }
            else -> null
        }
    }

    private fun queryFileInstance(fileInstance: FileInstance): MatrixCursor {
        val list = fileInstance.list()
        Log.i(TAG, "queryFileInstance: ${fileInstance.path} ${list.directories.size} ${list.files.size}")
        val matrixCursor = MatrixCursor(fileProjection)
        val singleton = MimeTypeMap.getSingleton()
        list.directories.forEach {
            matrixCursor.addRow(arrayOf(it.name, it.fullPath, 0, DocumentsContract.Document.MIME_TYPE_DIR))
        }
        list.files.forEach {
            matrixCursor.addRow(arrayOf(it.name, it.fullPath, it.size, singleton.getMimeTypeFromExtension(it.extension)))
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
        private val fileProjection = arrayOf(
            FileSystemProviderConstant.fileName,
            FileSystemProviderConstant.filePath,
            FileSystemProviderConstant.fileSize,
            FileSystemProviderConstant.fileMimeType
        )
    }
}