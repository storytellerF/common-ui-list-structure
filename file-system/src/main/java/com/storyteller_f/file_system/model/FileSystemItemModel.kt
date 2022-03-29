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
    var lastAccessTime: Long = 0
    var createdTime: Long = 0
    var size: Long = 0
    var formattedSize: String? = null
    var detail: String? = null
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
                ", detail='" + detail + '\'' +
                '}'
    }

    init {
        formattedLastModifiedTime = getTime(lastModifiedTime)
    }
}