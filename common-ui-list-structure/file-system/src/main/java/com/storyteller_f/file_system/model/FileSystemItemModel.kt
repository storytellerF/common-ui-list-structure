package com.storyteller_f.file_system.model

import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.text.SimpleDateFormat
import java.util.*

open class FileSystemItemModel(
    name: String,
    absolutePath: String,
    val isHidden: Boolean,
    val lastModifiedTime: Long,
    val isSymLink: Boolean,
) : FileSystemItemModelLite(name, absolutePath) {
    private val simpleDateFormat = SimpleDateFormat("yyyy:MM:dd hh:mm:ss sss", Locale.CHINA)
    val formattedLastModifiedTime: String
    var formattedLastAccessTime: String? = null
    var formattedCreatedTime: String? = null
    var lastAccessTime: Long = 0
        set(value) {
            formattedLastAccessTime = getTime(value)
            field = value
        }
    var createdTime: Long = 0
        set(value) {
            formattedCreatedTime = getTime(value)
            field = value
        }
    var size: Long = 0
    var formattedSize: String? = null
    var permissions: String? = null
    private fun getTime(time: Long): String {
        return simpleDateFormat.format(Date(time))
    }

    override fun toString(): String {
        return "FileSystemItemModel{" +
                "name='" + name + '\'' +
                ", absPath='" + fullPath + '\'' +
                ", isHidden=" + isHidden +
                ", size=" + size +
                ", lastAccessTime='" + lastModifiedTime + '\'' +
                ", permissions='" + permissions + '\'' +
                '}'
    }

    fun editAccessTime(childFile: File) {
        val fileSystemItemModel = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val basicFileAttributes = Files.readAttributes(childFile.toPath(), BasicFileAttributes::class.java)
                fileSystemItemModel.createdTime = basicFileAttributes.creationTime().toMillis()
                fileSystemItemModel.lastAccessTime = basicFileAttributes.lastAccessTime().toMillis()
            } catch (e: IOException) {
                Log.w(TAG, "list: 获取BasicFileAttribute失败" + childFile.absolutePath)
            }
        }
    }

    init {
        formattedLastModifiedTime = getTime(lastModifiedTime)
    }

    companion object {
        private const val TAG = "FileSystemItemModel"
    }
}