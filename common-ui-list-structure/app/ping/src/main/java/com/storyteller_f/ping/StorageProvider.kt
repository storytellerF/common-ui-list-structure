package com.storyteller_f.ping

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class StorageProvider : DocumentsProvider() {
    private val DEFAULT_ROOT_ID = "0"

    companion object {
        private val DEFAULT_ROOT_PROJECTION: Array<String> = arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_MIME_TYPES,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_SUMMARY,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES
        )
        private val DEFAULT_DOCUMENT_PROJECTION: Array<String> = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE
        )
        private const val TAG = "StorageProvider"

    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        Log.d(TAG, "queryRoots() called with: projection = $projection")
        val flags = DocumentsContract.Root.FLAG_LOCAL_ONLY or DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD

        return MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION).apply {
            newRow().apply {
                add(DocumentsContract.Root.COLUMN_ROOT_ID, DEFAULT_ROOT_ID)
                add(DocumentsContract.Root.COLUMN_MIME_TYPES, DocumentsContract.Document.MIME_TYPE_DIR)
                add(DocumentsContract.Root.COLUMN_FLAGS, flags)
                add(DocumentsContract.Root.COLUMN_ICON, R.drawable.ic_launcher_foreground)
                add(DocumentsContract.Root.COLUMN_TITLE, "ping")
                add(DocumentsContract.Root.COLUMN_SUMMARY, "your data")
                add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, "/")
            }
        }
    }

    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor {
        Log.d(TAG, "queryDocument() called with: documentId = $documentId, projection = $projection")
        return MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).apply {
            handleChild(documentId, false)
        }
    }

    private fun MatrixCursor.handleChild(documentId: String?, isChild: Boolean) {
        if (documentId != null) {
            context?.let {
                val root = it.filesDir.parentFile ?: return@let
                File(root, documentId).listFiles()?.forEach {
                    newRow().apply {
                        val subDocumentId = it.absolutePath.substring(root.absolutePath.length)
                        add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, if (isChild) subDocumentId else documentId)
                        val type = if (it.isFile) {
                            MimeTypeMap.getSingleton().getMimeTypeFromExtension(it.extension)
                        } else {
                            DocumentsContract.Document.MIME_TYPE_DIR
                        }
                        add(DocumentsContract.Document.COLUMN_MIME_TYPE, type)
                        val flags = 0
                        add(DocumentsContract.Document.COLUMN_FLAGS, flags)
                        add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, it.name)
                        add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, it.lastModified())
                        val size = if (it.isFile) {
                            it.length()
                        } else 0
                        add(DocumentsContract.Document.COLUMN_SIZE, size)
                    }
                }
            }
        }
    }

    override fun queryChildDocuments(parentDocumentId: String?, projection: Array<out String>?, sortOrder: String?): Cursor {
        Log.d(TAG, "queryChildDocuments() called with: parentDocumentId = $parentDocumentId, projection = $projection, sortOrder = $sortOrder")

        return MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).apply {
            handleChild(parentDocumentId, true)
        }
    }

    override fun openDocument(documentId: String?, mode: String?, signal: CancellationSignal?): ParcelFileDescriptor {
        Log.d(TAG, "openDocument() called with: documentId = $documentId, mode = $mode, signal = $signal")
        val accessMode: Int = ParcelFileDescriptor.parseMode(mode)

        val isWrite: Boolean = mode?.contains("w") ?: false
        val lc = context
        lc ?: throw Exception()
        val root = lc.filesDir?.parentFile
        val file = File(root, documentId ?: throw Exception())
        return if (isWrite) {
            val handler = Handler(lc.mainLooper ?: throw Exception())
            // Attach a close listener if the document is opened in write mode.
            try {
                ParcelFileDescriptor.open(file, accessMode, handler) {
                    // Update the file with the cloud server. The client is done writing.
                    Log.i(TAG, "A file with id $documentId has been closed! Time to update the server.")
                }
            } catch (e: IOException) {
                throw FileNotFoundException(
                    "Failed to open document with id $documentId and mode $mode"
                )
            }
        } else {
            ParcelFileDescriptor.open(file, accessMode)
        }
    }
}