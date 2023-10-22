package com.storyteller_f.ping

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.graphics.Bitmap
import android.graphics.Point
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.CancellationSignal
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.graphics.scale
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

class StorageProvider : DocumentsProvider() {

    companion object {
        private const val DEFAULT_ROOT_ID = "0"
        internal const val ELEMENT_ID = "ping"

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
        val flags =
            DocumentsContract.Root.FLAG_LOCAL_ONLY or DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD

        return MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION).apply {
            newRow().apply {
                add(DocumentsContract.Root.COLUMN_ROOT_ID, DEFAULT_ROOT_ID)
                add(
                    DocumentsContract.Root.COLUMN_MIME_TYPES,
                    DocumentsContract.Document.MIME_TYPE_DIR
                )
                add(DocumentsContract.Root.COLUMN_FLAGS, flags)
                add(DocumentsContract.Root.COLUMN_ICON, R.drawable.ic_launcher_foreground)
                add(DocumentsContract.Root.COLUMN_TITLE, context?.getString(R.string.app_name))
                add(DocumentsContract.Root.COLUMN_SUMMARY, "Your private data")
                add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, ELEMENT_ID)
            }
        }
    }

    override fun queryDocument(documentId: String?, projection: Array<out String>?): Cursor {
        Log.d(
            TAG,
            "queryDocument() called with: documentId = $documentId, projection = ${projection?.joinToString()}"
        )
        return MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).apply {
            val root = context.root ?: return@apply
            val file = getDocument(root, documentId)
            newRow().apply {
                fileRow(file, root)
            }
        }
    }

    private fun MatrixCursor.handleChild(documentId: String?) {
        if (documentId != null) {

            val root = context.root ?: return
            context?.let {
                File(root, getPath(documentId)).listFiles()?.forEach {
                    newRow().apply {
                        fileRow(it, root)
                    }
                }
            }
        }
    }

    override fun queryChildDocuments(
        parentDocumentId: String?,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        Log.d(
            TAG,
            "queryChildDocuments() called with: parentDocumentId = $parentDocumentId, projection = ${projection?.joinToString()}, sortOrder = $sortOrder"
        )

        return MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION).apply {
            handleChild(parentDocumentId)
        }
    }

    override fun openDocument(
        documentId: String?,
        mode: String?,
        signal: CancellationSignal?
    ): ParcelFileDescriptor {
        Log.d(
            TAG,
            "openDocument() called with: documentId = $documentId, mode = $mode, signal = $signal"
        )
        val accessMode: Int = ParcelFileDescriptor.parseMode(mode)

        val isWrite: Boolean = mode?.contains("w") ?: false
        val lc = context!!
        val root = lc.filesDir?.parentFile
        val file = File(root, documentId!!)
        return if (isWrite) {
            val handler = Handler(lc.mainLooper ?: throw Exception())
            // Attach a close listener if the document is opened in write mode.
            try {
                ParcelFileDescriptor.open(file, accessMode, handler) {
                    // Update the file with the cloud server. The client is done writing.
                    Log.i(
                        TAG,
                        "A file with id $documentId has been closed! Time to update the server."
                    )
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

    override fun openDocumentThumbnail(
        documentId: String?,
        sizeHint: Point?,
        signal: CancellationSignal?
    ): AssetFileDescriptor {
        val lc = context!!
        sizeHint!!
        val hash =
            MessageDigest.getInstance("md5").digest(documentId!!.toByteArray()).joinToString("") {
                "%02x".format(it)
            }
        val root = lc.filesDir?.parentFile!!
        val thumbFile = File(root, "cache/.storage-provider/.thumbnail/$hash.jpg")
        if (!thumbFile.exists()) {
            thumbFile.parentFile?.mkdirs()
            thumbFile.createNewFile()
            val fileOutputStream = FileOutputStream(thumbFile)
            val frameAtTime = firstFrame(root, documentId)
            var preHeight = frameAtTime.height
            var preWidth = frameAtTime.width
            while (preHeight >= sizeHint.y * 2 && preWidth >= sizeHint.x * 2) {
                preHeight /= 2
                preWidth /= 2
            }
            val scale = frameAtTime.scale(preWidth, preHeight)
            scale.compress(Bitmap.CompressFormat.JPEG, 90, fileOutputStream)
        }

        return AssetFileDescriptor(
            ParcelFileDescriptor.open(
                thumbFile,
                ParcelFileDescriptor.parseMode("r")
            ), 0, AssetFileDescriptor.UNKNOWN_LENGTH
        )
    }

    override fun isChildDocument(parentDocumentId: String?, documentId: String?): Boolean {
        if (parentDocumentId == null) return false
        if (documentId == null) return false
        return documentId.startsWith(parentDocumentId)
    }

    private fun firstFrame(root: File, documentId: String): Bitmap {
        val file = File(root, documentId)
        val mediaExtractor = MediaMetadataRetriever()
        mediaExtractor.setDataSource(file.absolutePath)
        return mediaExtractor.getFrameAtTime(0)!!
    }
}

val Context?.root: File?
    get() {
        return this?.filesDir?.parentFile
    }

fun getDocument(root: File, documentId: String?): File {
    val subPath = if (documentId == null || documentId == StorageProvider.ELEMENT_ID) "/"
    else documentId.substring(
        StorageProvider.ELEMENT_ID.length
    )
    return File(root, subPath)
}

fun MatrixCursor.RowBuilder.fileRow(it: File, root: File) {
    val subDocumentId = subDocumentId(it, root)
    add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, subDocumentId)
    val type = if (it.isFile) {
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(it.extension)
    } else {
        DocumentsContract.Document.MIME_TYPE_DIR
    }
    add(DocumentsContract.Document.COLUMN_MIME_TYPE, type)
    val copyFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        DocumentsContract.Document.FLAG_SUPPORTS_COPY
    } else {
        0
    }
    val thumbnailFlag = if (it.extension == "mp4") {
        DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL
    } else 0
    add(DocumentsContract.Document.COLUMN_FLAGS, copyFlag or thumbnailFlag)
    add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, it.name)
    add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, it.lastModified())
    val size = if (it.isFile) {
        it.length()
    } else 0
    add(DocumentsContract.Document.COLUMN_SIZE, size)
    Log.d("StorageProvider", "fileRow: $subDocumentId ${it.name}")
}

fun subDocumentId(it: File, root: File): String {
    return "${StorageProvider.ELEMENT_ID}${it.absolutePath.substring(root.absolutePath.length)}"
}

fun getPath(documentId: String): String {
    return documentId.substring(StorageProvider.ELEMENT_ID.length)
}