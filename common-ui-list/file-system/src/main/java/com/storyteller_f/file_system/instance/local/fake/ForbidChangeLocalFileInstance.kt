package com.storyteller_f.file_system.instance.local.fake

import android.net.Uri

/**
 * 禁止修改操作
 */
abstract class ForbidChangeLocalFileInstance(uri: Uri) :
    DirectoryLocalFileInstance(uri) {
    override fun createDirectory() = false

    override fun deleteFileOrEmptyDirectory() = false

    override fun rename(newName: String?) = false
}