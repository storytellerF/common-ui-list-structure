package com.storyteller_f.file_system_remote

import android.net.Uri
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import kotlinx.coroutines.runBlocking
import java.io.FileInputStream
import java.io.FileOutputStream

val webdavInstances = mutableMapOf<ShareSpec, WebDavInstance>()

class WebDavFileInstance(private val spec: ShareSpec, uri: Uri) : FileInstance(uri) {

    private val instance = getWebDavInstance()

    private fun getWebDavInstance(): WebDavInstance {
        return webdavInstances.getOrPut(spec) {
            WebDavInstance(spec)
        }
    }

    override suspend fun getFile(): FileItemModel {
        TODO("Not yet implemented")
    }

    override suspend fun getDirectory(): DirectoryItemModel {
        TODO("Not yet implemented")
    }

    override suspend fun getFileLength(): Long {
        TODO("Not yet implemented")
    }

    override suspend fun getFileInputStream(): FileInputStream {
        TODO("Not yet implemented")
    }

    override suspend fun getFileOutputStream(): FileOutputStream {
        TODO("Not yet implemented")
    }

    override suspend fun listInternal(fileItems: MutableList<FileItemModel>, directoryItems: MutableList<DirectoryItemModel>) {
        instance.list(path).forEach {
            val (file, child) = child(it.name)
            if (it.isFile)
                fileItems.add(FileItemModel(it.name, child, false, it.lastModified, false, file.extension))
            else
                directoryItems.add(DirectoryItemModel(it.name, child, false, it.lastModified, false))
        }
    }

    override suspend fun isFile(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun exists(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun isDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun deleteFileOrEmptyDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun rename(newName: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun toParent(): FileInstance {
        TODO("Not yet implemented")
    }

    override suspend fun getDirectorySize(): Long {
        TODO("Not yet implemented")
    }

    override suspend fun createFile(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun isHidden(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun createDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance {
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