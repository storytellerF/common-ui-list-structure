package com.storyteller_f.giant_explorer.service

import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.giant_explorer.database.RemoteSpec
import com.thegrizzlylabs.sardineandroid.DavResource
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

val sardines = mutableMapOf<RemoteSpec, WebDavInstance>()

class WebDavFileInstance(path: String, fileSystemRoot: String, val spec: RemoteSpec) : FileInstance(path, fileSystemRoot) {

    val instance = getWebDavInstance()

    private fun getWebDavInstance(): WebDavInstance {
        return sardines.getOrPut(spec) {
            WebDavInstance(spec)
        }
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
        instance.list("http://${spec.server}:${spec.port}$path")?.forEach {
            if (it.isDirectory) {
                directoryItems.add(DirectoryItemModel(it.name, it.path, false, it.modified.time, false))
            } else fileItems.add(FileItemModel(it.name, it.path, false, it.modified.time, false, File(it.path).extension))
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

class WebDavInstance(spec: RemoteSpec) {
    val instance = OkHttpSardine().apply {
        setCredentials(spec.user, spec.password)
    }

    fun list(path: String): MutableList<DavResource>? {
        return instance.list(path)
    }

}