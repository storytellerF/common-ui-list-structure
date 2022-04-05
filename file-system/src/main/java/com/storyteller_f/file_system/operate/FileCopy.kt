package com.storyteller_f.file_system.operate

import android.content.Context
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.local.RegularLocalFileInstance
import com.storyteller_f.file_system.message.Message
import com.storyteller_f.multi_core.StoppableTask
import java.lang.Exception
import java.nio.ByteBuffer
import java.util.concurrent.Callable

abstract class FileOperator(val task: StoppableTask, val fileInstance: FileInstance, val context: Context) : Callable<Boolean?> {
    var fileOperateListener: FileOperateListener? = null
}
/**
 * target 必须是一个目录
 */
abstract class FileOperatorAim(
    task: StoppableTask,
    fileInstance: FileInstance,
    val target: FileInstance,
    context: Context
) : FileOperator(task, fileInstance, context) {
    init {
        assert(target.isDirectory) {
            "target 必须是一个目录"
        }
    }
}

abstract class FileOperatorSelf(
    task: StoppableTask,
    fileInstance: FileInstance,
    context: Context
) : FileOperator(task, fileInstance, context)

class FileCopy(
    task: StoppableTask,
    fileInstance: FileInstance,
    target: FileInstance,
    context: Context
) : FileOperatorAim(task, fileInstance, target, context) {
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
            copyFileFaster(
                FileInstanceFactory.toChild(f, it.name, true, context, true),
                t
            )
        }
        listSafe.directories.forEach {
            if (needStop()) return false
            copyDirectoryFaster(
                FileInstanceFactory.toChild(f, it.name, false, context, true),
                FileInstanceFactory.toChild(t, it.name, false, context, true)
            )
        }
        fileOperateListener?.onOneDirectory(fileInstance, 0, Message("${f.name} success"))
        return true
    }

    private fun copyFileFaster(f: FileInstance, t: FileInstance): Boolean {
        try {
            val toChild = FileInstanceFactory.toChild(t, f.name, true, context, true)
            (f as RegularLocalFileInstance).fileInputStream.channel.use { int ->
                (toChild as RegularLocalFileInstance).fileOutputStream.channel.use { out ->
                    val byteArray = ByteBuffer.allocateDirect(1024)
                    while (int.read(byteArray) != -1) {
                        if (needStop()) return false
                        byteArray.flip()
                        out.write(byteArray)
                        byteArray.clear()
                    }
                    fileOperateListener?.onOneFile(f, 0, Message(""))
                    return true
                }

            }
        } catch (e: Exception) {
            e.printStackTrace()
            fileOperateListener?.onError(Message(e.message ?: "error"), 0)
        }
        return false
    }

    private fun needStop(): Boolean {
        if (task.needStop()) {
            fileOperateListener?.onError(Message("canceled"), 0)
            return true
        }
        return false
    }
}

class FileMove(
    task: StoppableTask,
    fileInstance: FileInstance,
    target: FileInstance,
    context: Context
) : FileOperatorAim(task, fileInstance, target, context), FileOperateListener {
    override fun call(): Boolean {
        return FileCopy(task, fileInstance, target, context).also {
            it.fileOperateListener = this
        }.call()
    }

    override fun onOneFile(fileInstance: FileInstance?, type: Int, message: Message?) {
        fileOperateListener?.onOneFile(fileInstance, type, Message("move success"))
        try {
            fileInstance?.deleteFileOrEmptyDirectory()
        } catch (e: Exception) {
            fileOperateListener?.onError(Message("delete ${fileInstance?.name} failed"), 0)
        }
    }

    override fun onOneDirectory(fileInstance: FileInstance?, type: Int, message: Message?) {
        fileOperateListener?.onOneDirectory(fileInstance, type, Message("move directory success"))
        try {
            fileInstance?.deleteFileOrEmptyDirectory()
        } catch (e: Exception) {
            fileOperateListener?.onError(Message("delete ${fileInstance?.name} failed"), 0)
        }
    }

    override fun onError(message: Message?, type: Int) {
        fileOperateListener?.onError(message, type)
    }
}

class FileMoveCmd(
    task: StoppableTask,
    fileInstance: FileInstance,
    target: FileInstance,
    context: Context
) : FileOperatorAim(task, fileInstance, target, context) {
    override fun call(): Boolean {
        val exec = Runtime.getRuntime().exec("mv ${fileInstance.path} ${target.path}")
        val waitFor = exec.waitFor()
        val b = waitFor != 0
        if (b) {
            fileOperateListener?.onError(Message("exec return $waitFor"), 0)
        } else {
            fileOperateListener?.onOneDirectory(fileInstance, 0, Message("success"))
        }
        return b
    }
}

class FileDelete(
    task: StoppableTask,
    fileInstance: FileInstance,
    context: Context
) : FileOperatorSelf(task, fileInstance, context) {
    init {
        assert(fileInstance.isDirectory)
    }

    override fun call(): Boolean {
        return deleteDirectory(fileInstance)
    }

    private fun deleteDirectory(fileInstance: FileInstance): Boolean {
        try {
            val listSafe = fileInstance.listSafe()
            if (listSafe.files.any {
                    val toChild =
                        FileInstanceFactory.toChild(fileInstance, it.name, true, context, false)
                    !toChild.deleteFileOrEmptyDirectory().apply {
                        if (this)
                            fileOperateListener?.onOneFile(
                                toChild,
                                0,
                                Message("delete ${it.name} success")
                            )
                        else
                            fileOperateListener?.onError(Message("delete ${it.name} failed"), 0)
                    }
                }) {
                return false
            }
            if (listSafe.directories.any {
                    val c = FileInstanceFactory.toChild(
                        fileInstance,
                        it.name,
                        false,
                        context,
                        false
                    )
                    !deleteDirectory(c).apply {
                        if (this)
                            fileOperateListener?.onOneFile(
                                c,
                                0,
                                Message("delete ${it.name} success")
                            )
                        else
                            fileOperateListener?.onError(Message("delete ${it.name} failed"), 0)
                    }
                }) {
                return false
            }
            return false
        } catch (e: Exception) {
            return false
        }

    }

}