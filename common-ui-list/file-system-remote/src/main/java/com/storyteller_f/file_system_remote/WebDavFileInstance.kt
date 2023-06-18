package com.storyteller_f.file_system_remote

import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

val webdavInstances = mutableMapOf<ShareSpec, WebDavInstance>()

class WebDavFileInstance(path: String, fileSystemRoot: String, val spec: ShareSpec) : FileInstance(path, fileSystemRoot) {

    val instance = getWebDavInstance()

    private fun getWebDavInstance(): WebDavInstance {
        return webdavInstances.getOrPut(spec) {
            WebDavInstance(spec)
        }
    }

    override fun getFile(): FileItemModel {
        TODO("Not yet implemented")
    }

    override fun getDirectory(): DirectoryItemModel {
        TODO("Not yet implemented")
    }

    override fun getBufferedReader(): BufferedReader {
        TODO("Not yet implemented")
    }

    override fun getBufferedWriter(): BufferedWriter {
        TODO("Not yet implemented")
    }

    override fun getFileInputStream(): FileInputStream {
        TODO("Not yet implemented")
    }

    override fun getFileOutputStream(): FileOutputStream {
        TODO("Not yet implemented")
    }

    override fun listInternal(fileItems: MutableList<FileItemModel>, directoryItems: MutableList<DirectoryItemModel>) {
        instance.list(path).forEach {
            if (it.isFile) fileItems.add(FileItemModel(it.name, it.path, false, it.lastModified, false, File(it.path).extension))
            else {
                directoryItems.add(DirectoryItemModel(it.name, it.path, false, it.lastModified, false))
            }
        }
    }

    override fun isFile(): Boolean {
        TODO("Not yet implemented")
    }

    override fun exists(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override fun deleteFileOrEmptyDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override fun rename(newName: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun toParent(): FileInstance {
        TODO("Not yet implemented")
    }

    override fun changeToParent() {
        TODO("Not yet implemented")
    }

    override fun getDirectorySize(): Long {
        TODO("Not yet implemented")
    }

    override fun createFile(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isHidden(): Boolean {
        TODO("Not yet implemented")
    }

    override fun createDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override fun toChild(name: String, isFile: Boolean, createWhenNotExists: Boolean): FileInstance {
        TODO("Not yet implemented")
    }

    override fun changeToChild(name: String, isFile: Boolean, createWhenNotExists: Boolean) {
        TODO("Not yet implemented")
    }

    override fun changeTo(path: String) {
        TODO("Not yet implemented")
    }
}

class WebDavInstance(spec: ShareSpec) {
    val instance = WebDavClient("http://${spec.server}:${spec.port}/${spec.share}", spec.user, spec.password)

    fun list(path: String): MutableList<WebDavDatum> {
        return runBlocking {
            instance.list(path)
        }
    }

}