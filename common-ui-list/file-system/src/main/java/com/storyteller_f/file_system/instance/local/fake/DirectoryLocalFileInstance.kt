package com.storyteller_f.file_system.instance.local.fake

import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.FileInstance

/**
 * 目录型，用于特殊类型
 */
abstract class DirectoryLocalFileInstance(path: String) : FileInstance(path, FileInstanceFactory.publicFileSystemRoot) {
    override fun getFileLength(): Long = -1L

    override fun getBufferedReader() = null

    override fun getBufferedWriter() = null

    override fun getFile() = null

    override fun isFile() = false

    override fun isDirectory() = true

    override fun createFile() = false
}