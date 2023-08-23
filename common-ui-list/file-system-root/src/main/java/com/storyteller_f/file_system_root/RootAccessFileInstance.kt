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

class RootAccessFileInstance(private val remote: FileSystemManager, uri: Uri) : FileInstance(uri) {

    private var extendedFile = remote.getFile(path)
    override suspend fun getFile(): FileItemModel {
        return FileItemModel(
            extendedFile.name,
            uri,
            extendedFile.isHidden,
            extendedFile.lastModified(),
            extendedFile.isSymlink,
            extendedFile.extension
        )
    }

    override suspend fun getDirectory(): DirectoryItemModel {
        return DirectoryItemModel(
            extendedFile.name,
            uri,
            extendedFile.isHidden,
            extendedFile.lastModified(),
            extendedFile.isSymlink
        )
    }

    override suspend fun getFileLength(): Long {
        return extendedFile.length()
    }

    override suspend fun getFileInputStream() = extendedFile.inputStream()

    override suspend fun getFileOutputStream() = extendedFile.outputStream()

    override suspend fun listInternal(
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

    override suspend fun isFile(): Boolean = extendedFile.isFile

    override suspend fun exists(): Boolean = extendedFile.exists()

    override suspend fun isDirectory(): Boolean = extendedFile.isDirectory

    override suspend fun deleteFileOrEmptyDirectory(): Boolean = extendedFile.delete()

    override suspend fun rename(newName: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun toParent(): FileInstance {
        val newUri = uri.buildUpon().path(extendedFile.parent!!).build()
        return RootAccessFileInstance(remote, newUri)
    }

    override suspend fun getDirectorySize(): Long {
        TODO("Not yet implemented")
    }

    override suspend fun createFile(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun isHidden(): Boolean = extendedFile.isHidden

    override suspend fun createDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun toChild(name: String, policy: FileCreatePolicy): FileInstance {
        val newUri = uri.buildUpon().path(File(extendedFile, name).absolutePath).build()
        return RootAccessFileInstance(remote, newUri)
    }

    override suspend fun isSymbolicLink(): Boolean = extendedFile.isSymlink

    companion object {
        const val rootFileSystemScheme = "root"
        var remote: FileSystemManager? = null

        val isReady get() = remote != null

        fun instance(uri: Uri): RootAccessFileInstance? {
            val r = remote ?: return null
            return RootAccessFileInstance(r, uri)
        }
    }
}