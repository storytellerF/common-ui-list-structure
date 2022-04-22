package com.storyteller_f.file_system.instance.local.fake

import android.content.Context
import android.os.Build
import androidx.annotation.WorkerThread
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.local.ForbidChangeDirectoryLocalFileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.FilesAndDirectories
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

class FakeDirectoryLocalFileInstance(path: String, val context: Context) :
    ForbidChangeDirectoryLocalFileInstance(path) {
    private val dy: MutableMap<String, List<String>> = mutableMapOf(
        "/data/user/0" to listOf(context.packageName),
        "/data/data" to listOf(context.packageName),
    )

    private val dyf: MutableMap<String, List<String>> = mutableMapOf(
        "/data/app" to context.packageManager.getInstalledApplications(0).map {
            it.packageName ?: "unknown"
        }
    )


    override fun getDirectory(): DirectoryItemModel {
        return DirectoryItemModel("/", "/", false, -1)
    }

    override fun getFileInputStream(): FileInputStream {
        TODO("Not yet implemented")
    }

    override fun getFileOutputStream(): FileOutputStream {
        TODO("Not yet implemented")
    }

    override fun getFileLength() = -1L

    @WorkerThread
    override fun list(): FilesAndDirectories {
        return FilesAndDirectories(
            dyf[path]?.map {
                FileItemModel(it, "$path/$it", false, File("$path/$it").lastModified()).apply {
                    size = File(context.packageManager.getApplicationInfo(it, 0).publicSourceDir).length()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            val basicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
                            createdTime = basicFileAttributes.creationTime().toMillis()
                            lastAccessTime = basicFileAttributes.lastAccessTime().toMillis()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                    }
                }
            }?.toMutableList() ?: mutableListOf(), (map[path] ?: dy[path])?.map {
                DirectoryItemModel(it, "$path/$it", false, File("$path/$it").lastModified())
            }?.toMutableList() ?: mutableListOf()
        )
    }

    override fun exists() = true


    override fun toParent(): FileInstance {
        TODO("Not yet implemented")
    }

    override fun changeToParent() {
        TODO("Not yet implemented")
    }

    override fun getDirectorySize(): Long {
        TODO("Not yet implemented")
    }

    override fun isHide() = false

    override fun toChild(name: String?, isFile: Boolean, reCreate: Boolean): FileInstance {
        return if (!isFile) {
            FakeDirectoryLocalFileInstance("$path/$name", context)
        } else throw Exception("不允许文件")
    }

    override fun changeToChild(name: String?, isFile: Boolean, reCreate: Boolean) {
        TODO("Not yet implemented")
    }

    override fun changeTo(path: String?) {
        TODO("Not yet implemented")
    }

    override fun getParent(): String {
        TODO("Not yet implemented")
    }

    override fun listSafe(): FilesAndDirectories {
        return list()
    }

    companion object {
        val map = mapOf(
            "" to listOf("sdcard", "storage", "data", "mnt", "system"),
            "/data" to listOf("user", "data", "app"),
            "/data/user" to listOf("0"),
        )
    }
}