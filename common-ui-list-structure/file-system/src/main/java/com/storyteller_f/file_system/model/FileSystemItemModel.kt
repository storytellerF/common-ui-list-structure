package com.storyteller_f.file_system.model

import java.text.SimpleDateFormat
import java.util.*

open class FileSystemItemModel(
    name: String,
    absolutePath: String,
    val isHidden: Boolean,
    val lastModifiedTime: Long
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

    init {
        formattedLastModifiedTime = getTime(lastModifiedTime)
    }
}