package com.storyteller_f.file_system.instance.local.fake

import android.net.Uri
import com.storyteller_f.file_system.instance.FileInstance

/**
 * 目录型，用于特殊类型
 */
abstract class DirectoryLocalFileInstance(uri: Uri) : FileInstance(uri) {
    override fun getFileLength(): Long = -1L

    override fun getFile() = null

    override fun isFile() = false

    override fun isDirectory() = true

    override fun createFile() = false
}