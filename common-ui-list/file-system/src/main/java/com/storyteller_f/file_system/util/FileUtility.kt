package com.storyteller_f.file_system.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.LocalFileSystemPrefix
import com.storyteller_f.file_system.instance.local.DocumentLocalFileInstance
import java.io.File
import java.nio.file.Files
import java.util.Locale
import java.util.Objects

object FileUtility {
    private const val TAG = "FileUtility"
    fun getPermissionStringByFile(file: File): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return getPermissions(file.isFile, file)
        }
        val w = file.canWrite()
        val e = file.canExecute()
        val r = file.canRead()
        return permissions(r, w, e, file.isFile)
    }

    fun getPermissions(file: DocumentFile): String {
        val w = file.canWrite()
        val r = file.canRead()
        return permissions(r, w, false, file.isFile)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun getPermissions(b: Boolean, file: File): String {
        val path = file.toPath()
        val w = Files.isWritable(path)
        val e = Files.isExecutable(path)
        val r = Files.isReadable(path)
        return String.format(
            Locale.CHINA,
            "%c%c%c%c",
            if (b) '-' else 'd',
            if (r) 'r' else '-',
            if (w) 'w' else '-',
            if (e) 'e' else '-'
        )
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private fun getStorageVolume(context: Context, storageManager: StorageManager): List<StorageVolume> {
        val storageVolumes = storageManager.storageVolumes
        for (volume in storageVolumes) {
            printStorageVolume(volume, context)
        }
        return storageVolumes
    }

    private fun printStorageVolume(storageVolume: StorageVolume, context: Context?) {
        val stringBuilder = StringBuilder()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val uuid = storageVolume.uuid
            stringBuilder.append("Uuid:").append(uuid).append("\n")
            val description = storageVolume.getDescription(context)
            stringBuilder.append("Description:").append(description).append("\n")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val directory = storageVolume.directory
            stringBuilder.append("directory:").append(directory).append("\n")
            val mediaStoreVolumeName = storageVolume.mediaStoreVolumeName
            stringBuilder.append("mediaStore:").append(mediaStoreVolumeName).append("\n")
            val state = storageVolume.state
            stringBuilder.append("state:").append(state).append("\n")
        }
        Log.i(TAG, "printStorageVolume: $stringBuilder")
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    fun getStorageVolume(context: Context): List<StorageVolume> {
        val storageManager = context.getSystemService(StorageManager::class.java)
        return getStorageVolume(context, storageManager)
    }

    fun getStorageCompat(context: Context): List<File> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            getStorageVolume(context).map { storageVolume: StorageVolume ->
                val uuid = storageVolume.uuid
                File(FileInstanceFactory.storagePath, volumePathName(uuid))
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val externalFilesDirs = context.externalCacheDirs
            externalFilesDirs.map {
                val absolutePath = it.absolutePath
                val endIndex = absolutePath.indexOf("Android")
                val path = absolutePath.substring(0, endIndex)
                File(path)
            }
        } else {
            val file = File("/storage/")
            file.listFiles()?.toList() ?: return listOf()
        }
    }

    fun volumePathName(uuid: String?): String =
        Objects.requireNonNullElse(uuid, "emulated")

    fun generateSAFRequestIntent(activity: Activity, prefix: LocalFileSystemPrefix): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val sm = activity.getSystemService(StorageManager::class.java)
            val volume = sm.getStorageVolume(File(prefix.key))
            if (volume != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return volume.createOpenDocumentTreeIntent()
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (prefix is LocalFileSystemPrefix.RootEmulated) {
                    val primary = DocumentsContract.buildRootUri(
                        DocumentLocalFileInstance.EXTERNAL_STORAGE_DOCUMENTS,
                        DocumentLocalFileInstance.EXTERNAL_STORAGE_DOCUMENTS_TREE
                    )
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, primary)
                } else if (prefix is LocalFileSystemPrefix.Mounted) {
                    val tree = DocumentLocalFileInstance.getMountedTree(prefix.key)
                    val primary = DocumentsContract.buildRootUri(
                        DocumentLocalFileInstance.EXTERNAL_STORAGE_DOCUMENTS,
                        tree
                    )
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, primary)
                }
            }
            return intent
        }
        return null
    }

    fun getExtension(name: String): String? {
        val index = name.lastIndexOf('.')
        return if (index == -1) null else name.substring(index + 1)
    }

    @Suppress("DEPRECATION")
    fun getSpace(prefix: String?): Long {
        val stat = StatFs(prefix)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            stat.availableBytes
        } else {
            stat.blockSize.toLong() * stat.availableBlocks.toLong()
        }
    }

    @Suppress("DEPRECATION")
    fun getFree(prefix: String?): Long {
        val stat = StatFs(prefix)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            stat.freeBytes
        } else {
            stat.blockSize.toLong() * stat.freeBlocks.toLong()
        }
    }

    @Suppress("DEPRECATION")
    fun getTotal(prefix: String?): Long {
        val stat = StatFs(prefix)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            stat.totalBytes
        } else {
            stat.blockSize.toLong() * stat.blockCount.toLong()
        }
    }

    fun permissions(r: Boolean, w: Boolean, e: Boolean, isFile: Boolean): String {
        return String.format(
            Locale.CHINA,
            "%c%c%c%c",
            if (isFile) '-' else 'd',
            if (r) 'r' else '-',
            if (w) 'w' else '-',
            if (e) 'e' else '-'
        )
    }
}
