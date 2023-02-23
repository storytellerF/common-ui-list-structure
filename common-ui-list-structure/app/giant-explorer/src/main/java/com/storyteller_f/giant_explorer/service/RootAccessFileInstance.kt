package com.storyteller_f.giant_explorer.service

import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.util.FileInstanceUtility
import com.storyteller_f.giant_explorer.control.remote
import com.topjohnwu.superuser.nio.ExtendedFile
import com.topjohnwu.superuser.nio.FileSystemManager
import java.io.*
import java.util.*

class RootAccessFileInstance(path: String, remote: FileSystemManager) : FileInstance(path) {

    private var extendedFile = remote.getFile(path)

    override fun getBufferedReader(): BufferedReader = extendedFile.bufferedReader()

    override fun getBufferedWriter(): BufferedWriter = extendedFile.bufferedWriter()

    override fun getFileInputStream(): FileInputStream = extendedFile.inputStream()

    override fun getFileOutputStream(): FileOutputStream = extendedFile.outputStream()

    override fun list(
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
        return RootAccessFileInstance(File(path).parent!!, remote!!)
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
        return RootAccessFileInstance(File(file, name).absolutePath, remote!!)
    }

    override fun changeToChild(name: String, isFile: Boolean, createWhenNotExists: Boolean) {
        val tempFile = File(file, name)
        val childFile = remote?.getFile(file.absolutePath)
        childFile?.let {
            file = tempFile
            extendedFile = childFile
        }
    }

    override fun changeTo(path: String) {
        val tempFile = File(path)
        val childFile = remote?.getFile(path)
        childFile?.let {
            file = tempFile
            extendedFile = childFile
        }
    }

    override fun getParent(): String? = extendedFile.parent
}