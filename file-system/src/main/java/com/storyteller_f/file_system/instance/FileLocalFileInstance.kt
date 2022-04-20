package com.storyteller_f.file_system.instance

import com.storyteller_f.file_system.model.FilesAndDirectories

abstract class FileLocalFileInstance : FileInstance() {
    override fun isFile() = true
    override fun isDirectory() = false
    override fun list(): FilesAndDirectories {
        TODO("Not yet implemented")
    }

    override fun listSafe(): FilesAndDirectories {
        TODO("Not yet implemented")
    }
}