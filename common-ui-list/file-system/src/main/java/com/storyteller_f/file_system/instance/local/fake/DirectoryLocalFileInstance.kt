package com.storyteller_f.file_system.instance.local.fake

import android.net.Uri
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.FileItemModel

/**
 * 目录型，用于特殊类型
 */
abstract class DirectoryLocalFileInstance(uri: Uri) : FileInstance(uri) {
    override suspend fun getFileLength(): Long = -1

    override suspend fun getFile(): FileItemModel = TODO()

    override suspend fun isFile() = false

    override suspend fun isDirectory() = true

    override suspend fun createFile() = false
}
