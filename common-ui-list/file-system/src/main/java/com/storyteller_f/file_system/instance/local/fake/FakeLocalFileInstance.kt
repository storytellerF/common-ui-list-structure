package com.storyteller_f.file_system.instance.local.fake

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Process
import android.os.UserManager
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

fun Context.getMyId() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    getSystemService(UserManager::class.java)
        .getSerialNumberForUser(Process.myUserHandle())
} else {
    TODO("VERSION.SDK_INT < M")
}

/**
 * 预定义的用于无法访问的中间目录，不能标识一个文件类型
 */
class FakeLocalFileInstance(val context: Context, uri: Uri) :
    ForbidChangeLocalFileInstance(uri) {
    private val myId = context.getMyId()

    @SuppressLint("SdCardPath")
    private val presetDirectories: MutableMap<String, List<String>> = mutableMapOf(
        "/data/user/$myId" to listOf(context.packageName),
        "/data/data" to listOf(context.packageName),
        FileInstanceFactory.emulatedRootPath to listOf(myId.toString()),
        "/data/user" to listOf(myId.toString()),
    )

    private val presetFiles: MutableMap<String, List<String>> = mutableMapOf(
        "/data/app" to context.packageManager.getInstalledApplicationsCompat(0).mapNotNull {
            it.packageName
        }
    )

    override suspend fun getDirectory(): DirectoryItemModel =
        DirectoryItemModel("/", uri, false, -1, false)

    override suspend fun getFileInputStream(): FileInputStream = TODO("Not yet implemented")

    override suspend fun getFileOutputStream(): FileOutputStream = TODO("Not yet implemented")

    override suspend fun getFileLength(): Long = -1

    @WorkerThread
    override suspend fun listInternal(
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

    override suspend fun exists() = true

    override suspend fun toParent(): FileInstance {
        TODO("Not yet implemented")
    }

    override suspend fun getDirectorySize(): Long = TODO("Not yet implemented")

    override suspend fun isHidden(): Boolean = false

    override suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance {
        val (_, child) = child(name)
        return FakeLocalFileInstance(context, child)
    }

    companion object {
        val presetSystemDirectories = mapOf(
            "/" to listOf("sdcard", "storage", "data", "mnt", "system"),
            "/data" to listOf("user", "data", "app", "local"),
            "/storage" to listOf("self"),
            "/storage/self" to listOf("primary")
        )

        val symLink = listOf("bin", "sdcard", "etc")
    }
}
