package com.storyteller_f.file_system.operate

import android.content.Context
import com.storyteller_f.common_ktx.exceptionMessage
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.FileCreatePolicy.Create
import com.storyteller_f.file_system.instance.FileCreatePolicy.NotCreate
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.message.Message
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.multi_core.StoppableTask
import kotlinx.coroutines.yield
import java.nio.ByteBuffer

interface SuspendCallable<T> {
    suspend fun call(): T
}

/**
 * 返回应该是任务是否成功
 */
abstract class AbstractFileOperation(val task: StoppableTask, val fileInstance: FileInstance, val context: Context) : SuspendCallable<Boolean>, FileOperationListener {
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

    fun bind(fileOperationListener: FileOperationListener): AbstractFileOperation {
        this.fileOperationListener = fileOperationListener
        return this
    }
}

/**
 * target 必须是一个目录
 */
abstract class ScopeFileOperation(
    task: StoppableTask, fileInstance: FileInstance, val target: FileInstance, context: Context
) : AbstractFileOperation(task, fileInstance, context)

abstract class MultiScopeFileOperation(
    task: StoppableTask, fileInstance: FileInstance, context: Context
) : AbstractFileOperation(task, fileInstance, context)

open class ScopeFileCopyOp(
    task: StoppableTask, fileInstance: FileInstance, target: FileInstance, context: Context
) : ScopeFileOperation(task, fileInstance, target, context) {
    override suspend fun call(): Boolean {
        return if (fileInstance.isFile()) {
            //新建一个文件
            copyFileFaster(fileInstance, target)
        } else {
            copyDirectoryFaster(
                fileInstance, FileInstanceFactory.toChild(
                    context,
                    target,
                    fileInstance.name,
                    Create(false)
                )
            )
        }
    }

    private suspend fun copyDirectoryFaster(f: FileInstance, t: FileInstance): Boolean {
        val listSafe = f.list()
        listSafe.files.forEach {
            yield()
            copyFileFaster(FileInstanceFactory.toChild(context, f, it.name, Create(true)), t)
        }
        listSafe.directories.forEach {
            yield()
            copyDirectoryFaster(
                FileInstanceFactory.toChild(context, f, it.name, Create(false)), FileInstanceFactory.toChild(
                    context,
                    t,
                    it.name,
                    Create(false)
                )
            )
        }
        notifyDirectoryDone(fileInstance, Message("${f.name} success"), 0)
        return true
    }

    open suspend fun notifyDirectoryDone(fileInstance: FileInstance, message: Message, i: Int) {
        onDirectoryDone(fileInstance, message, i)
    }

    open suspend fun notifyFileDone(f: FileInstance, message: Message, fileLength: Long, i: Int) {
        onFileDone(fileInstance, message, fileLength, i)
    }

    private suspend fun copyFileFaster(f: FileInstance, t: FileInstance): Boolean {
        try {
            val toChild = FileInstanceFactory.toChild(context, t, f.name, Create(true))
            f.getFileInputStream().channel.use { int ->
                (toChild).getFileOutputStream().channel.use { out ->
                    val byteBuffer = ByteBuffer.allocateDirect(1024)
                    while (int.read(byteBuffer) != -1) {
                        yield()
                        byteBuffer.flip()
                        out.write(byteBuffer)
                        byteBuffer.clear()
                    }
                    notifyFileDone(f, Message(""), f.getFileLength(), 0)
                    return true
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
            onError(Message(e.message ?: "error"), 0)
        }
        return false
    }

}

class ScopeFileMoveOp(
    task: StoppableTask, fileInstance: FileInstance, target: FileInstance, context: Context
) : ScopeFileCopyOp(task, fileInstance, target, context), FileOperationListener {

    override suspend fun notifyFileDone(f: FileInstance, message: Message, fileLength: Long, i: Int) {
        super.notifyFileDone(f, message, fileLength, i)
        try {
            fileInstance.deleteFileOrEmptyDirectory()
        } catch (e: Exception) {
            fileOperationListener?.onError(Message("delete ${fileInstance.name} failed"), 0)
        }
    }

    override suspend fun notifyDirectoryDone(
        fileInstance: FileInstance,
        message: Message,
        i: Int
    ) {
        super.notifyDirectoryDone(fileInstance, message, i)
        try {
            fileInstance.deleteFileOrEmptyDirectory()
        } catch (e: Exception) {
            fileOperationListener?.onError(Message("delete ${fileInstance.name} failed ${e.exceptionMessage}"), 0)
        }
    }
}

class ScopeFileMoveOpInShell(
    task: StoppableTask, fileInstance: FileInstance, target: FileInstance, context: Context
) : ScopeFileOperation(task, fileInstance, target, context) {
    override suspend fun call(): Boolean {
        val exec = Runtime.getRuntime().exec("mv ${fileInstance.path} ${target.path}")
        val waitFor = exec.waitFor()
        val cmdFailed = waitFor != 0
        when {
            cmdFailed -> onError(Message("exec return $waitFor"), 0)
            target.isFile() -> onFileDone(target, Message("success"), target.getFileLength(), 0)
            else -> onDirectoryDone(fileInstance, Message("success"), 0)
        }
        return !cmdFailed
    }
}

class FileDeleteOp(
    task: StoppableTask, fileInstance: FileInstance, context: Context
) : MultiScopeFileOperation(task, fileInstance, context) {

    override suspend fun call(): Boolean {
        return deleteDirectory(fileInstance)
    }

    /**
     * @return 是否成功
     */
    private suspend fun deleteDirectory(fileInstance: FileInstance): Boolean {
        try {
            val listSafe = fileInstance.list()
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

    private suspend fun deleteChildDirectory(fileInstance: FileInstance, it: DirectoryItemModel): Boolean {
        val childDirectory = FileInstanceFactory.toChild(
            context, fileInstance, it.name, NotCreate
        )
        val deleteDirectory = deleteDirectory(childDirectory)
        if (deleteDirectory)
            onDirectoryDone(
                childDirectory, Message("delete ${it.name} success"), 0
            )
        else onError(Message("delete ${it.name} failed"), 0)
        return deleteDirectory
    }

    private suspend fun deleteChildFile(fileInstance: FileInstance, it: FileItemModel): Boolean {
        val childFile = FileInstanceFactory.toChild(context, fileInstance, it.name, NotCreate)
        val deleteFileOrEmptyDirectory = childFile.deleteFileOrEmptyDirectory()
        if (deleteFileOrEmptyDirectory)
            onFileDone(
                childFile, Message("delete ${it.name} success"), fileInstance.getFileLength(), 0
            )
        else onError(Message("delete ${it.name} failed"), 0)
        return deleteFileOrEmptyDirectory
    }

}