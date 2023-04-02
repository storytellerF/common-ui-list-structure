package com.storyteller_f.file_system.instance.local.fake

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.BaseContextFileInstance
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import java.io.*

/**
 * 标识一个apk 文件
 */
class AppLocalFileInstance(context: Context, path: String) : BaseContextFileInstance(context, path, FileInstanceFactory.publicFileSystemRoot) {
    override fun getFile() = FileItemModel(name, path, false, 0, false, "apk")

    override fun getDirectory(): DirectoryItemModel {
        TODO("Not yet implemented")
    }

    private val publicSourceDir: String = context.packageManager.getApplicationInfoCompat(name, 0).publicSourceDir

    override fun getFileLength() = File(publicSourceDir).length()

    override fun getBufferedReader() = BufferedReader(FileReader(publicSourceDir))

    override fun getBufferedWriter() = BufferedWriter(FileWriter(publicSourceDir))

    override fun getFileInputStream() = FileInputStream(publicSourceDir)

    override fun getFileOutputStream() = FileOutputStream(publicSourceDir)

    override fun list(fileItems: MutableList<FileItemModel>, directoryItems: MutableList<DirectoryItemModel>) {
        TODO("Not yet implemented")
    }

    override fun isFile() = true

    override fun exists() = true

    override fun isDirectory() = false

    override fun deleteFileOrEmptyDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override fun rename(newName: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun toParent(): FileInstance {
        TODO("Not yet implemented")
    }

    override fun changeToParent() {
        TODO("Not yet implemented")
    }

    override fun getDirectorySize(): Long {
        TODO("Not yet implemented")
    }

    override fun createFile(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isHidden(): Boolean {
        TODO("Not yet implemented")
    }

    override fun createDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override fun toChild(name: String, isFile: Boolean, createWhenNotExists: Boolean): FileInstance {
        TODO("Not yet implemented")
    }

    override fun changeToChild(name: String, isFile: Boolean, createWhenNotExists: Boolean) {
        TODO("Not yet implemented")
    }

    override fun changeTo(path: String) {
        TODO("Not yet implemented")
    }

    override fun getParent(): String {
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