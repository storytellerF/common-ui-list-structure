package com.storyteller_f.file_system_remote

import android.net.Uri
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

val webdavInstances = mutableMapOf<ShareSpec, WebDavInstance>()

class WebDavFileInstance(private val spec: ShareSpec, uri: Uri) : FileInstance(uri) {

    val instance = getWebDavInstance()

    private fun getWebDavInstance(): WebDavInstance {
        return webdavInstances.getOrPut(spec) {
            WebDavInstance(spec)
        }
    }

    override val file: FileItemModel
        get() {
            TODO("Not yet implemented")
        }

    override val directory: DirectoryItemModel
        get() {
            TODO("Not yet implemented")
        }

    override val fileLength: Long
        get() {
            TODO("Not yet implemented")
        }

    override val fileInputStream: FileInputStream
        get() {
            TODO("Not yet implemented")
        }

    override val fileOutputStream: FileOutputStream
        get() {
            TODO("Not yet implemented")
        }

    override fun listInternal(fileItems: MutableList<FileItemModel>, directoryItems: MutableList<DirectoryItemModel>) {
        instance.list(path).forEach {
            val (file, child) = child(it.name)
            if (it.isFile)
                fileItems.add(FileItemModel(it.name, child, false, it.lastModified, false, file.extension))
            else
                directoryItems.add(DirectoryItemModel(it.name, child, false, it.lastModified, false))
        }
    }

    override val isFile: Boolean
        get() {
            TODO("Not yet implemented")
        }

    override fun exists(): Boolean {
        TODO("Not yet implemented")
    }

    override val isDirectory: Boolean
        get() {
            TODO("Not yet implemented")
        }

    override fun deleteFileOrEmptyDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override fun rename(newName: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun toParent(): FileInstance {
        TODO("Not yet implemented")
    }

    override fun changeToParent() {
        TODO("Not yet implemented")
    }

    override val directorySize: Long
        get() {
            TODO("Not yet implemented")
        }

    override fun createFile(): Boolean {
        TODO("Not yet implemented")
    }

    override val isHidden: Boolean
        get() {
            TODO("Not yet implemented")
        }

    override fun createDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override fun toChild(name: String, policy: FileCreatePolicy): FileInstance {
        TODO("Not yet implemented")
    }

    override fun changeToChild(name: String, policy: FileCreatePolicy) {
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