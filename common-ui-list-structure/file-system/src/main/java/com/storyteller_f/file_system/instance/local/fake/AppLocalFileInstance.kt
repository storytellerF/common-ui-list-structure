package com.storyteller_f.file_system.instance.local.fake

import android.content.Context
import com.storyteller_f.file_system.instance.BaseContextFileInstance
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.FilesAndDirectories
import java.io.*

class AppLocalFileInstance(context: Context, path: String) : BaseContextFileInstance(context, path) {
    override fun getFile() = FileItemModel(name, path, false, 0, "apk")

    override fun getDirectory(): DirectoryItemModel {
        TODO("Not yet implemented")
    }

    val publicSourceDir = context.packageManager.getApplicationInfo(name, 0).publicSourceDir

    override fun getFileLength() = File(publicSourceDir).length()


    override fun getBufferedOutputStream() = BufferedOutputStream(fileOutputStream)

    override fun getBufferedInputSteam() = BufferedInputStream(fileInputStream)

    override fun getBufferedReader() = BufferedReader(FileReader(publicSourceDir))

    override fun getBufferedWriter() = BufferedWriter(FileWriter(publicSourceDir))

    override fun getFileInputStream() = FileInputStream(publicSourceDir)

    override fun getFileOutputStream() = FileOutputStream(publicSourceDir)

    override fun list(): FilesAndDirectories {
        TODO("Not yet implemented")
    }

    override fun isFile() = true

    override fun exists() = true

    override fun isDirectory() = false

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

    override fun listSafe(): FilesAndDirectories {
        TODO("Not yet implemented")
    }
}