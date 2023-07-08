package com.storyteller_f.file_system.instance.local.fake

import android.net.Uri
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.FileItemModel

/**
 * 目录型，用于特殊类型
 */
abstract class DirectoryLocalFileInstance(uri: Uri) : FileInstance(uri) {
    override val fileLength: Long
        get() = -1

    override val file: FileItemModel
        get() = TODO()

    override val isFile get() = false

    override val isDirectory get() = true

    override fun createFile() = false
}