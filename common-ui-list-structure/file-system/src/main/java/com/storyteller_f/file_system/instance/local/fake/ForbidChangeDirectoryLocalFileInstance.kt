package com.storyteller_f.file_system.instance.local.fake

import com.storyteller_f.file_system.instance.local.fake.DirectoryLocalFileInstance

/**
 * 禁止修改操作
 */
abstract class ForbidChangeDirectoryLocalFileInstance(path: String) :
    DirectoryLocalFileInstance(path) {
    override fun createDirectory() = false

    override fun deleteFileOrEmptyDirectory() = false

    override fun rename(newName: String?) = false
}