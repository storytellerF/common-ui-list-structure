package com.storyteller_f.file_system.instance.local

abstract class ForbidChangeDirectoryLocalFileInstance(path: String) :
    DirectoryLocalFileInstance(path) {
    override fun createDirectory() = false

    override fun deleteFileOrEmptyDirectory() = false

    override fun rename(newName: String?) = false
}