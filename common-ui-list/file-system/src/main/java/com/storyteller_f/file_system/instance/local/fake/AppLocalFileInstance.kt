package com.storyteller_f.file_system.instance.local.fake

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import com.storyteller_f.file_system.instance.BaseContextFileInstance
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import java.io.*

/**
 * 标识一个apk 文件
 */
class AppLocalFileInstance(context: Context, uri: Uri) : BaseContextFileInstance(context, uri) {
    override suspend fun getFile() = FileItemModel(name, uri, false, 0, false, "apk")

    override suspend fun getDirectory(): DirectoryItemModel {
        TODO("Not yet implemented")
    }

    private val publicSourceDir: String = context.packageManager.getApplicationInfoCompat(name, 0).publicSourceDir

    override suspend fun getFileLength() = File(publicSourceDir).length()

    override suspend fun getFileInputStream() = FileInputStream(publicSourceDir)

    override suspend fun getFileOutputStream() = FileOutputStream(publicSourceDir)

    override suspend fun listInternal(fileItems: MutableList<FileItemModel>, directoryItems: MutableList<DirectoryItemModel>) {
        TODO("Not yet implemented")
    }

    override suspend fun isFile() = true

    override suspend fun exists() = true

    override suspend fun isDirectory() = false

    override suspend fun deleteFileOrEmptyDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun rename(newName: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun toParent(): FileInstance {
        TODO("Not yet implemented")
    }

    override suspend fun getDirectorySize(): Long {
        TODO("Not yet implemented")
    }

    override suspend fun createFile(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun isHidden(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun createDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance {
        TODO("Not yet implemented")
    }

}

@Suppress("DEPRECATION")
fun PackageManager.getApplicationInfoCompat(packageName: String, flag: Long): ApplicationInfo {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flag))
    } else getApplicationInfo(packageName, flag.toInt())
}

@SuppressLint("QueryPermissionsNeeded")
@Suppress("DEPRECATION")
fun PackageManager.getInstalledApplicationsCompat(flag: Long): MutableList<ApplicationInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flag))
    } else getInstalledApplications(flag.toInt())
}