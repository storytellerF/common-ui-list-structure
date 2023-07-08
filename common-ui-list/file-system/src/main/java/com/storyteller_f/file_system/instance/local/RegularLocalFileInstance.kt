package com.storyteller_f.file_system.instance.local

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.WorkerThread
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileCreatePolicy.*
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.util.FileInstanceUtility
import com.storyteller_f.file_system.util.FileUtility
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files

@Suppress("unused")
class RegularLocalFileInstance(context: Context, uri: Uri) : LocalFileInstance(context, uri) {
    private var innerFile = File(path)

    @Throws(IOException::class)
    override fun createFile(): Boolean {
        return if (innerFile.exists()) true else innerFile.createNewFile()
    }

    override val isHidden: Boolean
        get() = innerFile.isHidden

    override fun createDirectory(): Boolean {
        return if (innerFile.exists()) true else innerFile.mkdirs()
    }

    @Throws(Exception::class)
    override fun toChild(name: String, policy: FileCreatePolicy): LocalFileInstance {
        val subFile = File(innerFile, name)
        val uri = getUri(subFile)
        val internalFileInstance = RegularLocalFileInstance(context, uri)
        //检查目标文件是否存在
        checkChildExistsOtherwiseCreate(subFile, policy)
        return internalFileInstance
    }

    @Throws(Exception::class)
    private fun checkChildExistsOtherwiseCreate(file: File, policy: FileCreatePolicy) {
        if (!exists()) {
            throw Exception("当前文件或者文件夹不存在。path:$path")
        } else if (isFile) throw Exception("当前是一个文件，无法向下操作") else if (!file.exists()) {
            if (policy is Create) {
                if (policy.isFile) {
                    if (!file.createNewFile()) throw Exception("新建文件失败")
                } else if (!file.mkdirs()) throw Exception("新建文件失败")
            } else throw Exception("不存在，且不能创建")
        }
    }

    @Throws(Exception::class)
    override fun changeToChild(name: String, policy: FileCreatePolicy) {
        Log.d(TAG, "changeToChild() called with: name = [$name], policy = [$policy]")
        checkChildExistsOtherwiseCreate(innerFile, policy)
        innerFile = File(innerFile, name)
    }

    override fun changeTo(path: String) {
        if (path == this.path) return
        innerFile = File(path)
    }

    @get:Throws(FileNotFoundException::class)
    override val fileInputStream: FileInputStream
        get() = FileInputStream(innerFile)

    @get:Throws(FileNotFoundException::class)
    override val fileOutputStream: FileOutputStream
        get() = FileOutputStream(innerFile)

    override val file: FileItemModel
        get() {
            val fileItemModel = FileItemModel(innerFile.name, uri, innerFile.isHidden, innerFile.lastModified(), false, FileUtility.getExtension(name))
            fileItemModel.editAccessTime(innerFile)
            return fileItemModel
        }

    override val directory: DirectoryItemModel
        get() {
            val directoryItemModel = DirectoryItemModel(innerFile.name, uri, innerFile.isHidden, innerFile.lastModified(), false)
            directoryItemModel.editAccessTime(innerFile)
            return directoryItemModel
        }
    override val fileLength: Long
        get() = innerFile.length()

    @WorkerThread
    public override fun listInternal(fileItems: MutableList<FileItemModel>, directoryItems: MutableList<DirectoryItemModel>) {
        val listFiles = innerFile.listFiles() //获取子文件
        if (listFiles != null) {
            for (childFile in listFiles) {
                val child = child(childFile.name)
                val permissions = FileUtility.getPermissionStringByFile(childFile)
                // 判断是否为文件夹
                (if (childFile.isDirectory) {
                    FileInstanceUtility.addDirectory(directoryItems, child, permissions)
                } else {
                    FileInstanceUtility.addFile(fileItems, child, permissions)
                })?.editAccessTime(childFile)
            }
        }
    }

    override val isFile: Boolean
        get() {
            return innerFile.isFile
        }

    override fun exists(): Boolean {
        return innerFile.exists()
    }

    override val isDirectory: Boolean
        get() {
            return innerFile.isDirectory
        }

    override fun deleteFileOrEmptyDirectory(): Boolean {
        return innerFile.delete()
    }

    override fun rename(newName: String): Boolean {
        return innerFile.renameTo(File(newName))
    }

    override fun toParent(): LocalFileInstance {
        return RegularLocalFileInstance(context, getUri(innerFile.parentFile!!))
    }

    override fun changeToParent() {
        innerFile = innerFile.parentFile!!
    }

    override val directorySize: Long
        get() = getFileSize(innerFile)

    @WorkerThread
    private fun getFileSize(file: File): Long {
        var size: Long = 0
        val files = file.listFiles() ?: return 0
        for (f in files) {
            if (needStop()) break
            size += if (f.isFile) {
                f.length()
            } else {
                getFileSize(f)
            }
        }
        return size
    }

    override val isSymbolicLink: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Files.isSymbolicLink(innerFile.toPath())
        } else try {
            innerFile.absolutePath == innerFile.canonicalPath
        } catch (e: IOException) {
            false
        }

    companion object {
        private const val TAG = "ExternalFileInstance"
        private fun getUri(subFile: File): Uri {
            return Uri.Builder().scheme("file").path(subFile.path).build()
        }
    }
}
