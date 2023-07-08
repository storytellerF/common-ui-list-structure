package com.storyteller_f.file_system.instance.local.fake

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.annotation.WorkerThread
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.util.FileUtility
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * 预定义的用于无法访问的中间目录，不能标识一个文件类型
 */
class FakeLocalFileInstance(val context: Context, uri: Uri) :
    ForbidChangeLocalFileInstance(uri) {
    @SuppressLint("SdCardPath")
    private val presetDirectories: MutableMap<String, List<String>> = mutableMapOf(
        "/data/user/0" to listOf(context.packageName),
        "/data/data" to listOf(context.packageName),
        FileInstanceFactory.emulatedRootPath to listOf("0")
    )

    private val presetFiles: MutableMap<String, List<String>> = mutableMapOf(
        "/data/app" to context.packageManager.getInstalledApplicationsCompat(0).mapNotNull {
            it.packageName
        }
    )

    override val directory: DirectoryItemModel
        get() = DirectoryItemModel("/", uri, false, -1, false)

    override val fileInputStream: FileInputStream
        get() = TODO("Not yet implemented")

    override val fileOutputStream: FileOutputStream
        get() = TODO("Not yet implemented")

    override val fileLength: Long
        get() = -1

    @WorkerThread
    override fun listInternal(
        fileItems: MutableList<FileItemModel>,
        directoryItems: MutableList<DirectoryItemModel>
    ) {
        presetFiles[path]?.map { packageName ->
            val (file, child) = child(packageName)
            val length = getAppSize(packageName)
            val lastModifiedTime = file.lastModified()
            FileItemModel(packageName, child, false, lastModifiedTime, isSymLink = false).apply {
                size = length
                editAccessTime(file)
            }
        }?.forEach(fileItems::add)

        (presetSystemDirectories[path] ?: presetDirectories[path])?.map {
            val (file, child) = child(it)
            DirectoryItemModel(it, child, false, file.lastModified(), symLink.contains(it))
        }?.forEach(directoryItems::add)

        if (path == FileInstanceFactory.storagePath) {
            storageVolumes().forEach(directoryItems::add)
        }
    }

    private fun getAppSize(packageName: String): Long {
        return File(
            context.packageManager.getApplicationInfoCompat(
                packageName,
                0
            ).publicSourceDir
        ).length()
    }

    private fun storageVolumes(): List<DirectoryItemModel> {
        return FileUtility.getStorageCompat(context).map {
            val (file, child) = child(it.name)
            DirectoryItemModel(it.name, child, false, file.lastModified(), false)
        }
    }

    override fun exists() = true


    override fun toParent(): FileInstance {
        TODO("Not yet implemented")
    }

    override fun changeToParent() {
        TODO("Not yet implemented")
    }

    override val directorySize: Long
        get() = TODO("Not yet implemented")

    override val isHidden: Boolean
        get() = false

    override fun toChild(name: String, policy: FileCreatePolicy): FileInstance {
        val (_, child) = child(name)
        return FakeLocalFileInstance(context, child)
    }

    override fun changeToChild(name: String, policy: FileCreatePolicy) {
        TODO("Not yet implemented")
    }

    override fun changeTo(path: String) {
        TODO("Not yet implemented")
    }

    override val parent: String?
        get() = super.parent

    companion object {
        val presetSystemDirectories = mapOf(
            "/" to listOf("sdcard", "storage", "data", "mnt", "system"),
            "/data" to listOf("user", "data", "app"),
            "/data/user" to listOf("0"),
            "/storage" to listOf("self"),
            "/storage/self" to listOf("primary")
        )

        val symLink = listOf("bin", "sdcard", "etc")
    }
}