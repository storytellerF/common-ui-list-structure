package com.storyteller_f.file_system.operate

import android.content.Context
import com.storyteller_f.common_ktx.exceptionMessage
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.message.Message
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.multi_core.StoppableTask
import java.nio.ByteBuffer
import java.util.concurrent.Callable

/**
 * 返回应该是任务是否成功
 */
abstract class FileOp(val task: StoppableTask, val fileInstance: FileInstance, val context: Context) : Callable<Boolean>, FileOperationListener {
    var fileOperationListener: FileOperationListener? = null

    override fun onError(message: Message?, type: Int) {
        fileOperationListener?.onError(message, type)
    }

    override fun onDirectoryDone(fileInstance: FileInstance?, message: Message?, type: Int) {
        fileOperationListener?.onDirectoryDone(fileInstance, message, type)
    }

    override fun onFileDone(fileInstance: FileInstance?, message: Message?, size: Long, type: Int) {
        fileOperationListener?.onFileDone(fileInstance, message, size, type)
    }

    fun bind(fileOperationListener: FileOperationListener): FileOp {
        this.fileOperationListener = fileOperationListener
        return this
    }
}

/**
 * target 必须是一个目录
 */
abstract class FileOpInSpec(
    task: StoppableTask, fileInstance: FileInstance, val target: FileInstance, context: Context
) : FileOp(task, fileInstance, context) {
    init {
        assert(target.isDirectory) {
            "target 必须是一个目录"
        }
    }
}

abstract class FileOpInSitu(
    task: StoppableTask, fileInstance: FileInstance, context: Context
) : FileOp(task, fileInstance, context)

class FileCopyOp(
    task: StoppableTask, fileInstance: FileInstance, target: FileInstance, context: Context
) : FileOpInSpec(task, fileInstance, target, context) {
    override fun call(): Boolean {
        return if (fileInstance.isFile) {
            //新建一个文件
            copyFileFaster(fileInstance, target)
        } else {
            copyDirectoryFaster(fileInstance, FileInstanceFactory.toChild(target, fileInstance.name, false, context, true))
        }
    }

    private fun copyDirectoryFaster(f: FileInstance, t: FileInstance): Boolean {
        val listSafe = f.listSafe()
        listSafe.files.forEach {
            if (needStop()) return false
            copyFileFaster(FileInstanceFactory.toChild(f, it.name, true, context, true), t)
        }
        listSafe.directories.forEach {
            if (needStop()) return false
            copyDirectoryFaster(
                FileInstanceFactory.toChild(f, it.name, false, context, true), FileInstanceFactory.toChild(t, it.name, false, context, true)
            )
        }
        onDirectoryDone(fileInstance, Message("${f.name} success"), 0)
        return true
    }

    private fun copyFileFaster(f: FileInstance, t: FileInstance): Boolean {
        Thread.sleep(500)
        try {
            val toChild = FileInstanceFactory.toChild(t, f.name, true, context, true)
            f.fileInputStream.channel.use { int ->
                (toChild).fileOutputStream.channel.use { out ->
                    val byteBuffer = ByteBuffer.allocateDirect(1024)
                    while (int.read(byteBuffer) != -1) {
                        if (needStop()) return false
                        byteBuffer.flip()
                        out.write(byteBuffer)
                        byteBuffer.clear()
                    }
                    onFileDone(f, Message(""), f.fileLength, 0)
                    return true
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
            onError(Message(e.message ?: "error"), 0)
        }
        return false
    }

    private fun needStop(): Boolean {
        if (task.needStop()) {
            onError(Message("canceled"), 0)
            return true
        }
        return false
    }
}

class FileMoveOp(
    task: StoppableTask, fileInstance: FileInstance, target: FileInstance, context: Context
) : FileOpInSpec(task, fileInstance, target, context), FileOperationListener {
    override fun call(): Boolean {
        return FileCopyOp(task, fileInstance, target, context).also {
            it.fileOperationListener = this
        }.call()
    }

    override fun onFileDone(fileInstance: FileInstance?, message: Message?, size: Long, type: Int) {
        fileOperationListener?.onFileDone(fileInstance, Message("move success"), size, type)
        try {
            fileInstance?.deleteFileOrEmptyDirectory()
        } catch (e: Exception) {
            fileOperationListener?.onError(Message("delete ${fileInstance?.name} failed"), 0)
        }
    }

    override fun onDirectoryDone(fileInstance: FileInstance?, message: Message?, type: Int) {
        fileOperationListener?.onDirectoryDone(fileInstance, Message("move directory success"), type)
        try {
            fileInstance?.deleteFileOrEmptyDirectory()
        } catch (e: Exception) {
            fileOperationListener?.onError(Message("delete ${fileInstance?.name} failed ${e.exceptionMessage}"), 0)
        }
    }
}

class FileMoveOpInShell(
    task: StoppableTask, fileInstance: FileInstance, target: FileInstance, context: Context
) : FileOpInSpec(task, fileInstance, target, context) {
    override fun call(): Boolean {
        val exec = Runtime.getRuntime().exec("mv ${fileInstance.path} ${target.path}")
        val waitFor = exec.waitFor()
        val cmdFailed = waitFor != 0
        Thread.sleep(500)
        when {
            cmdFailed -> onError(Message("exec return $waitFor"), 0)
            target.isFile -> onFileDone(target, Message("success"), target.fileLength, 0)
            else -> onDirectoryDone(fileInstance, Message("success"), 0)
        }
        return !cmdFailed
    }
}

class FileDeleteOp(
    task: StoppableTask, fileInstance: FileInstance, context: Context
) : FileOpInSitu(task, fileInstance, context) {

    override fun call(): Boolean {
        return deleteDirectory(fileInstance)
    }

    /**
     * @return 是否成功
     */
    private fun deleteDirectory(fileInstance: FileInstance): Boolean {
        try {
            val listSafe = fileInstance.listSafe()
            if (listSafe.files.any {
                    !deleteChildFile(fileInstance, it)
                }) {
                //如果失败提前结束
                return false
            }
            if (listSafe.directories.any {
                    !deleteChildDirectory(fileInstance, it)
                }) {
                //如果失败提前结束
                return false
            }
            //删除当前空文件夹
            val deleteCurrentDirectory = fileInstance.deleteFileOrEmptyDirectory()
            if (deleteCurrentDirectory)
                onDirectoryDone(
                    fileInstance, Message("delete ${fileInstance.name} success"), 0
                )
            else onError(Message("delete ${fileInstance.name} failed"), 0)
            return deleteCurrentDirectory
        } catch (_: Exception) {
            return false
        }

    }

    private fun deleteChildDirectory(fileInstance: FileInstance, it: DirectoryItemModel): Boolean {
        Thread.sleep(500)
        val childDirectory = FileInstanceFactory.toChild(
            fileInstance, it.name, false, context, false
        )
        val deleteDirectory = deleteDirectory(childDirectory)
        if (deleteDirectory)
            onDirectoryDone(
                childDirectory, Message("delete ${it.name} success"), 0
            )
        else onError(Message("delete ${it.name} failed"), 0)
        return deleteDirectory
    }

    private fun deleteChildFile(fileInstance: FileInstance, it: FileItemModel): Boolean {
        Thread.sleep(500)
        val childFile = FileInstanceFactory.toChild(fileInstance, it.name, true, context, false)
        val deleteFileOrEmptyDirectory = childFile.deleteFileOrEmptyDirectory()
        if (deleteFileOrEmptyDirectory)
            onFileDone(
                childFile, Message("delete ${it.name} success"), fileInstance.fileLength, 0
            )
        else onError(Message("delete ${it.name} failed"), 0)
        return deleteFileOrEmptyDirectory
    }

}