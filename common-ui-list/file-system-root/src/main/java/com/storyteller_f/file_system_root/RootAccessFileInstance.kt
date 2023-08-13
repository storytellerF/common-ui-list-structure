package com.storyteller_f.file_system_root

import android.net.Uri
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.util.FileInstanceUtility
import com.topjohnwu.superuser.nio.ExtendedFile
import com.topjohnwu.superuser.nio.FileSystemManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class RootAccessFileInstance(private val remote: FileSystemManager, uri: Uri) : FileInstance(uri) {

    private var extendedFile = remote.getFile(path)
    override val file: FileItemModel
        get() {
            return FileItemModel(extendedFile.name, uri, extendedFile.isHidden, extendedFile.lastModified(), extendedFile.isSymlink, extendedFile.extension)
        }

    override val directory: DirectoryItemModel
        get() {
            return DirectoryItemModel(extendedFile.name, uri, extendedFile.isHidden, extendedFile.lastModified(), extendedFile.isSymlink)
        }

    override val fileLength: Long
        get() {
            return extendedFile.length()
        }

    override val fileInputStream get() = extendedFile.inputStream()

    override val fileOutputStream
        get() = extendedFile.outputStream()

    override fun listInternal(
        fileItems: MutableList<FileItemModel>,
        directoryItems: MutableList<DirectoryItemModel>
    ) {
        val listFiles = extendedFile.listFiles()
        listFiles?.forEach {
            val permissions = it.permissions()
            val (_, child) = child(it.name)
            val pair = it to child
            if (it.isFile) {
                FileInstanceUtility.addFile(fileItems, pair, permissions)
            } else if (it.isDirectory) {
                FileInstanceUtility.addDirectory(directoryItems, pair, permissions)
            }
        }
    }

    private fun ExtendedFile.permissions(): String {
        val w = canWrite()
        val e = canExecute()
        val r = canRead()
        return com.storyteller_f.file_system.util.FileUtility.permissions(r, w, e, isFile)
    }

    override val isFile: Boolean
        get() = extendedFile.isFile

    override fun exists(): Boolean = extendedFile.exists()

    override val isDirectory: Boolean
        get() = extendedFile.isDirectory

    override fun deleteFileOrEmptyDirectory(): Boolean = extendedFile.delete()

    override fun rename(newName: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun toParent(): FileInstance {
        val newUri = uri.buildUpon().path(extendedFile.parent!!).build()
        return RootAccessFileInstance(remote, newUri)
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
        get() = extendedFile.isHidden

    override fun createDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override fun toChild(name: String, policy: FileCreatePolicy): FileInstance {
        val newUri = uri.buildUpon().path(File(extendedFile, name).absolutePath).build()
        return RootAccessFileInstance(remote, newUri)
    }

    override fun changeToChild(name: String, policy: FileCreatePolicy) {
        val childFile = remote.getFile(extendedFile.absolutePath)
        extendedFile = childFile
    }

    override fun changeTo(path: String) {
        val childFile = remote.getFile(path)
        extendedFile = childFile
    }

    override val parent: String?
        get() = extendedFile.parent

    override val isSymbolicLink: Boolean
        get() = extendedFile.isSymlink

    companion object {
        const val rootFileSystemScheme = "root"
        var remote: FileSystemManager? = null
    }
}