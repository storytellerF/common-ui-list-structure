package com.storyteller_f.file_system.instance.local.fake

/**
 * 禁止修改操作
 */
abstract class ForbidChangeDirectoryLocalFileInstance(path: String) :
    DirectoryLocalFileInstance(path) {
    override fun createDirectory() = false

    override fun deleteFileOrEmptyDirectory() = false

    override fun rename(newName: String?) = false
}