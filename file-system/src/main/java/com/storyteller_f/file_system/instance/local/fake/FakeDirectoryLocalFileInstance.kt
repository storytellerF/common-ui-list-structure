package com.storyteller_f.file_system.instance.local.fake

import android.content.Context
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.local.ForbidChangeDirectoryLocalFileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FilesAndDirectories
import java.lang.Exception

class FakeDirectoryLocalFileInstance(path: String, val context: Context) :
    ForbidChangeDirectoryLocalFileInstance(path) {
    val dy: MutableMap<String, MutableList<String>> = mutableMapOf(
        "/data/user/0" to mutableListOf(context.packageName),
        "/data/data" to mutableListOf(context.packageName)
    )

    override fun getDirectory(): DirectoryItemModel {
        return DirectoryItemModel("/", "/", false, -1)
    }

    override fun getFileLength() = -1L

    override fun list(): FilesAndDirectories {
        return FilesAndDirectories(
            mutableListOf(), (map[path] ?: dy[path])?.map {
                DirectoryItemModel(it, "$path/$it", false, -1)
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
            "" to listOf("sdcard", "storage", "data", "mnt"),
            "/data" to listOf("user", "data", "app"),
            "/data/user" to listOf("0")
        )
    }
}