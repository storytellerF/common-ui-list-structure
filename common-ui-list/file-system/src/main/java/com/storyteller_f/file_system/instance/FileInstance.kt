package com.storyteller_f.file_system.instance

import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.core.util.ObjectsCompat
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.file_system.model.FilesAndDirectories
import com.storyteller_f.multi_core.StoppableTask
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

//todo getChannel
//todo file descriptor
abstract class FileInstance(var uri: Uri) {
    private var task: StoppableTask? = null

    init {
        assert(path.trim { it <= ' ' }.isNotEmpty())
    }

    @get:WorkerThread
    abstract val file: FileItemModel

    @get:WorkerThread
    abstract val directory: DirectoryItemModel

    @get:WorkerThread
    val fileSystemItem: FileSystemItemModel
        get() = if (isFile) file else directory

    @get:WorkerThread
    open val name: String
        get() = File(path).name

    @get:WorkerThread
    abstract val fileLength: Long

    @get:WorkerThread
    abstract val fileInputStream: FileInputStream

    @get:WorkerThread
    abstract val fileOutputStream: FileOutputStream

    /**
     * 获取父路径
     *
     * @return 父路径
     */
    @get:WorkerThread
    open val parent: String?
        get() = File(path).parent

    @get:WorkerThread
    open val isSymbolicLink: Boolean
        get() = false

    @get:WorkerThread
    val isSoftLink: Boolean
        get() = false

    @get:WorkerThread
    val isHardLink: Boolean
        get() = false

    /**
     * 是否是文件
     *
     * @return true 代表是文件
     */
    @get:WorkerThread
    abstract val isFile: Boolean

    @get:WorkerThread
    abstract val isDirectory: Boolean

    @get:WorkerThread
    abstract val directorySize: Long

    @get:WorkerThread
    val path: String
        get() = uri.path!!

    @get:WorkerThread
    abstract val isHidden: Boolean

    /**
     * 应该仅用于目录。可能会抛出异常，内部不会处理。
     */
    @WorkerThread
    protected abstract fun listInternal(fileItems: MutableList<FileItemModel>, directoryItems: MutableList<DirectoryItemModel>)

    @WorkerThread
    fun list(): FilesAndDirectories {
        val filesAndDirectories = FilesAndDirectories(buildFilesContainer(), buildDirectoryContainer())
        listInternal(filesAndDirectories.files, filesAndDirectories.directories)
        return filesAndDirectories
    }

    /**
     * 是否存在
     *
     * @return true代表存在
     */
    @WorkerThread
    abstract fun exists(): Boolean

    @WorkerThread
    abstract fun createFile(): Boolean

    @WorkerThread
    abstract fun createDirectory(): Boolean

    /**
     * 调用者只能是一个路径
     * 如果目标文件或者文件夹不存在，将会自动创建，因为在这种状态下，新建文件速度快，特别是外部存储目录
     * 不应该考虑能否转换成功
     *
     * @param name                名称
     * @return 返回子对象
     */
    @WorkerThread
    abstract fun toChild(name: String, policy: FileCreatePolicy): FileInstance?

    /**
     * 移动指针，指向他的父文件夹
     * 不应该考虑能否转换成功
     *
     * @return 返回他的文件夹
     */
    @WorkerThread
    abstract fun toParent(): FileInstance

    /**
     * 删除当前文件
     *
     * @return 返回是否删除成功
     */
    @WorkerThread
    abstract fun deleteFileOrEmptyDirectory(): Boolean

    /**
     * 重命名当前文件
     *
     * @param newName 新的文件名，不包含路径
     * @return 返回是否重命名成功
     */
    @WorkerThread
    abstract fun rename(newName: String): Boolean

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as FileInstance
        return ObjectsCompat.equals(uri, that.uri)
    }

    override fun hashCode(): Int {
        return ObjectsCompat.hash(uri)
    }

    private fun buildFilesContainer(): MutableList<FileItemModel> {
        return mutableListOf()
    }

    private fun buildDirectoryContainer(): MutableList<DirectoryItemModel> {
        return mutableListOf()
    }

    protected fun needStop(): Boolean {
        if (Thread.currentThread().isInterrupted) return true
        return if (task != null) task!!.needStop() else false
    }

    protected fun child(it: String): Pair<File, Uri> {
        val file = File(path, it)
        val child = uri.buildUpon().path(file.absolutePath).build()
        return Pair(file, child)
    }
}
