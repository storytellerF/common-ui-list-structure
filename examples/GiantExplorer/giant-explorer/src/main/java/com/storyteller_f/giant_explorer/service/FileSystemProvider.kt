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
import androidx.core.net.toUri
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system_ktx.getFileInstance
import com.storyteller_f.giant_explorer.FileSystemProviderResolver
import com.storyteller_f.plugin_core.FileSystemProviderConstant
import kotlinx.coroutines.runBlocking
import java.io.File


class FileSystemProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        return true
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val path = FileSystemProviderResolver.resolvePath(uri) ?: return null
        return ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        val c = context ?: return null
        val filePath = FileSystemProviderResolver.resolvePath(uri) ?: return null
        Log.i(TAG, "query: file path:$filePath $uri")
        val fileInstance = getFileInstance(c, File(filePath).toUri())
        return runBlocking {
            if (fileInstance.isFile()) {
                MatrixCursor(fileProjection).apply {
                    val file = fileInstance.getFile()
                    addRow(arrayOf(file.name, file.fullPath, fileInstance.getFileLength()))
                }
            } else {
                queryFileInstanceChild(fileInstance)
            }
        }
    }

    private fun queryFileInstanceChild(fileInstance: FileInstance): MatrixCursor {
        val list = runBlocking {
            fileInstance.list()
        }
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