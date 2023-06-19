package com.storyteller_f.file_system.instance.local

import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.util.FileInstanceUtility
import com.topjohnwu.superuser.nio.ExtendedFile
import com.topjohnwu.superuser.nio.FileSystemManager
import java.io.*

class RootAccessFileInstance(path: String, private val remote: FileSystemManager) : FileInstance(path, FileInstanceFactory.rootFileSystemRoot) {

    private var extendedFile = remote.getFile(path)
    override fun getFile(): FileItemModel {
        return FileItemModel(extendedFile.name, extendedFile.absolutePath, extendedFile.isHidden, extendedFile.lastModified(), extendedFile.isSymlink, extendedFile.extension)
    }

    override fun getDirectory(): DirectoryItemModel {
        return DirectoryItemModel(extendedFile.name, extendedFile.absolutePath, extendedFile.isHidden, extendedFile.lastModified(), extendedFile.isSymlink)
    }

    override fun getFileInputStream(): FileInputStream = extendedFile.inputStream()

    override fun getFileOutputStream(): FileOutputStream = extendedFile.outputStream()

    override fun listInternal(
        fileItems: MutableList<FileItemModel>,
        directoryItems: MutableList<DirectoryItemModel>
    ) {
        val listFiles = extendedFile.listFiles()
        listFiles?.forEach {
            val format = it.permissions()
            if (it.isFile) {
                FileInstanceUtility.addFile(fileItems, it, format)
            } else if (it.isDirectory) {
                FileInstanceUtility.addDirectory(directoryItems, it, format)
            }
        }
    }

    private fun ExtendedFile.permissions(): String {
        val w = canWrite()
        val e = canExecute()
        val r = canRead()
        return com.storyteller_f.file_system.util.permissions(r, w, e, isFile)
    }

    override fun isFile(): Boolean = extendedFile.isFile

    override fun exists(): Boolean = extendedFile.exists()

    override fun isDirectory(): Boolean = extendedFile.isDirectory

    override fun deleteFileOrEmptyDirectory(): Boolean = extendedFile.delete()

    override fun rename(newName: String?): Boolean {
        TODO("Not yet implemented")
    }

    override fun toParent(): FileInstance {
        return RootAccessFileInstance(File(path).parent!!, remote)
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

    override fun isHidden() = extendedFile.isHidden

    override fun createDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override fun toChild(name: String, isFile: Boolean, createWhenNotExists: Boolean): FileInstance {
        return RootAccessFileInstance(File(file, name).absolutePath, remote)
    }

    override fun changeToChild(name: String, isFile: Boolean, createWhenNotExists: Boolean) {
        val tempFile = File(file, name)
        val childFile = remote.getFile(file.absolutePath)
        file = tempFile
        extendedFile = childFile
    }

    override fun changeTo(path: String) {
        val tempFile = File(path)
        val childFile = remote.getFile(path)
        file = tempFile
        extendedFile = childFile
    }

    override fun getParent(): String? = extendedFile.parent

    override fun isSymbolicLink(): Boolean {
        return extendedFile.isSymlink
    }
}