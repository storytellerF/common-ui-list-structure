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
    override val file
        get() = FileItemModel(name, uri, false, 0, false, "apk")

    override val directory: DirectoryItemModel
        get() {
            TODO("Not yet implemented")
        }

    private val publicSourceDir: String = context.packageManager.getApplicationInfoCompat(name, 0).publicSourceDir

    override val fileLength get() = File(publicSourceDir).length()

    override val fileInputStream get() = FileInputStream(publicSourceDir)

    override val fileOutputStream get() = FileOutputStream(publicSourceDir)

    override fun listInternal(fileItems: MutableList<FileItemModel>, directoryItems: MutableList<DirectoryItemModel>) {
        TODO("Not yet implemented")
    }

    override val isFile get() = true

    override fun exists() = true

    override val isDirectory get() = false

    override fun deleteFileOrEmptyDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override fun rename(newName: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun toParent(): FileInstance {
        TODO("Not yet implemented")
    }

    override fun changeToParent() {
        TODO("Not yet implemented")
    }

    override val directorySize: Long
        get() {
            TODO("Not yet implemented")
        }

    override fun createFile(): Boolean {
        TODO("Not yet implemented")
    }

    override val isHidden: Boolean
        get() {
            TODO("Not yet implemented")
        }

    override fun createDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override fun toChild(name: String, policy: FileCreatePolicy): FileInstance {
        TODO("Not yet implemented")
    }

    override fun changeToChild(name: String, policy: FileCreatePolicy) {
        TODO("Not yet implemented")
    }

    override fun changeTo(path: String) {
        TODO("Not yet implemented")
    }

    override val parent: String
        get() {
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