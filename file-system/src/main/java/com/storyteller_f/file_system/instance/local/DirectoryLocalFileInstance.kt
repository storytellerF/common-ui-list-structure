package com.storyteller_f.file_system.instance.local

import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.FileItemModel
import java.io.BufferedInputStream

abstract class DirectoryLocalFileInstance(path: String) : FileInstance(path) {
    override fun getFileLength(): Long = -1L

    override fun getBufferedOutputStream() = null

    override fun getBufferedInputSteam() = null

    override fun getBufferedReader() = null

    override fun getBufferedWriter() = null

    override fun getFile() = null

    override fun isFile() = false

    override fun isDirectory() = true

    override fun createFile() = false
}