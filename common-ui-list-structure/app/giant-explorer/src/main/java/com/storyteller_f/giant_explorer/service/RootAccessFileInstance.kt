package com.storyteller_f.giant_explorer.service

import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.FilesAndDirectories
import com.topjohnwu.superuser.nio.ExtendedFile
import com.topjohnwu.superuser.nio.FileSystemManager
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

class RootAccessFileInstance(path: String, remote: FileSystemManager) : FileInstance(path) {

    private val extendedFile = remote.getFile(path)

    override fun getBufferedReader(): BufferedReader = extendedFile.bufferedReader()

    override fun getBufferedWriter(): BufferedWriter = extendedFile.bufferedWriter()

    override fun getFileInputStream(): FileInputStream = extendedFile.inputStream()

    override fun getFileOutputStream(): FileOutputStream = extendedFile.outputStream()

    override fun list(): FilesAndDirectories {
        val listFiles = extendedFile.listFiles()
        val files = mutableListOf<FileItemModel>()
        val directories = mutableListOf<DirectoryItemModel>()
        listFiles?.forEach {
            (if (it.isFile) {
                addFile(files, extendedFile.absolutePath, it)
            } else if (it.isDirectory) {
                addDirectory(directories, extendedFile.absolutePath, it)
            } else null)?.let { model ->
                val format = it.permissions()
                model.permissions = format
            }
        }
        return FilesAndDirectories(files, directories)
    }

    private fun ExtendedFile.permissions(): String {
        val w = canWrite()
        val e = canExecute()
        val r = canRead()
        return String.format(Locale.CHINA, "%c%c%c%c", if (isFile) '-' else 'd', if (r) 'r' else '-', if (w) 'w' else '-', if (e) 'e' else '-')
    }

    override fun isFile(): Boolean = extendedFile.isFile

    override fun exists(): Boolean = extendedFile.exists()

    override fun isDirectory(): Boolean = extendedFile.isDirectory

    override fun deleteFileOrEmptyDirectory(): Boolean = extendedFile.delete()

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

    override fun isHidden() = extendedFile.isHidden

    override fun createDirectory(): Boolean {
        TODO("Not yet implemented")
    }

    override fun toChild(name: String?, isFile: Boolean, reCreate: Boolean): FileInstance {
        TODO("Not yet implemented")
    }

    override fun changeToChild(name: String?, isFile: Boolean, reCreate: Boolean) {
        TODO("Not yet implemented")
    }

    override fun changeTo(path: String?) {
        TODO("Not yet implemented")
    }

    override fun getParent(): String {
        TODO("Not yet implemented")
    }

    override fun listSafe() = list()
}