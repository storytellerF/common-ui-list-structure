package com.storyteller_f.giant_explorer.service

import android.content.Context
import android.util.Log
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.message.Message
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.file_system.operate.FileCopy
import com.storyteller_f.file_system.operate.FileDelete
import com.storyteller_f.file_system.operate.FileMoveCmd
import com.storyteller_f.file_system.operate.FileOperateListener
import com.storyteller_f.multi_core.StoppableTask
import java.util.concurrent.Callable

abstract class FileOperateWorker internal constructor(
    val context: Context, private val fileCount: Int, private val folderCount: Int, val size: Long,
    val focused: FileSystemItemModel, val key: String
) : Callable<Boolean>, FileOperateListener, StoppableTask {
    constructor(context: Context, focused: FileSystemItemModel, task: Task, key: String) : this(context, task.fileCount, task.folderCount, task.size, focused, key)

    var fileOperationProgressListener: FileOperationProgressListener? = null
    var leftFileCount = fileCount
    var leftFolderCount = folderCount
    var leftSize = size
    private fun emitCurrentStateMessage() {
        fileOperationProgressListener?.onLeft(leftFileCount, leftFolderCount, leftSize, key)
        fileOperationProgressListener?.onProgress(progress, key)
    }

    override fun onFileDone(fileInstance: FileInstance?, type: Int, message: Message?, size: Long) {
        leftFileCount--
        leftSize -= size
        emitCurrentStateMessage()
    }

    override fun onDirectoryDone(fileInstance: FileInstance?, type: Int, message: Message?) {
        leftFolderCount--
        emitCurrentStateMessage()
    }

    override fun onError(message: Message?, type: Int) {
        fileOperationProgressListener?.onDetail(message?.name + message?.get(), Log.ERROR, key)
    }


    override fun needStop(): Boolean {
        return Thread.currentThread().isInterrupted
    }

    protected fun emitDetailMessage(detail: String, level: Int) {
        fileOperationProgressListener?.onDetail(detail, level, key)
    }
    protected fun emitStateMessage(tip: String) {
        fileOperationProgressListener?.onState(tip, key)
    }

    abstract val description: String?

    open val progress: Int
        get() {
            val sumCount = fileCount + folderCount
            val completedCount = sumCount - leftFolderCount - leftFileCount
            return (completedCount * 1.0 / sumCount * 100).toInt()
        }

    interface FileOperationProgressListener {
        /**
         * 进度改变
         *
         * @param progress 新的进度
         */
        fun onProgress(progress: Int, key: String)

        /**
         * 正在做的工作
         *
         * @param state 新的状态信息
         */
        fun onState(state: String?, key: String)

        /**
         * 进入某个文件夹
         *
         * @param tip 新的提示
         */
        fun onTip(tip: String?, key: String)

        /**
         * 需要展示的详细信息
         */
        fun onDetail(detail: String?, level: Int, key: String)

        /**
         * 还剩余的任务
         */
        fun onLeft(fileCount: Int, folderCount: Int, size: Long, key: String)

        /**
         * 任务完成，可以刷新页面
         *
         * @param dest
         */
        fun onComplete(dest: String?, isSuccess: Boolean, key: String)
    }

    open class DefaultProgressListener : FileOperationProgressListener {
        override fun onProgress(progress: Int, key: String) {

        }

        override fun onState(state: String?, key: String) {
        }

        override fun onTip(tip: String?, key: String) {
        }

        override fun onDetail(detail: String?, level: Int, key: String) {
        }

        override fun onLeft(fileCount: Int, folderCount: Int, size: Long, key: String) {
        }

        override fun onComplete(dest: String?, isSuccess: Boolean, key: String) {
        }


    }

}

class CopyImpl(
    context: Context, private val detectorTasks: List<FileSystemItemModel>, task: Task,
    focused: FileSystemItemModel, private val isMove: Boolean, private val dest: FileInstance, key: String
) : FileOperateWorker(context, focused, task, key) {
    override val description: String
        get() {
            val taskName: String
            val fileName: String = detectorTasks[0].name
            taskName = if (detectorTasks.size == 1) {
                fileName
            } else {
                fileName + "等" + detectorTasks.size + "个文件"
            }
            return (if (isMove) "移动" else "复制") + taskName + "到" + dest.name
        }

    override fun call(): Boolean {
        val isSuccess = if (!detectorTasks.any {
                if (needStop()) {
                    emitDetailMessage("已暂停", Log.WARN)
                    return false
                }
                val fileInstance = FileInstanceFactory.getFileInstance(it.fullPath, context)
                emitStateMessage("处理${fileInstance.path}")
                if (isMove)
                    !FileMoveCmd(this, fileInstance, dest, context).apply {
                        fileOperateListener = this@CopyImpl
                    }.call()
                else
                    !FileCopy(this, fileInstance, dest, context).apply {
                        fileOperateListener = this@CopyImpl
                    }.call()
            }) {
            emitDetailMessage("error", Log.ERROR)
            true
        } else false
        fileOperationProgressListener?.onComplete(
            dest.path, isSuccess, key
        )
        return isSuccess
    }

    override val progress: Int
        get() {
            val doneSize: Long = size - leftSize
            return (doneSize * 100.0 / size).toInt()
        }

}

class DeleteImpl(
    context: Context, private val detectorTasks: List<FileSystemItemModel>,
    task: Task,
    focused: FileSystemItemModel, key: String
) : FileOperateWorker(context, focused, task, key) {

    override val description: String
        get() {
            return if (detectorTasks.size == 1) {
                "删除" + focused.name
            } else {
                "删除" + focused.name + "等" + detectorTasks.size.toString() + "个文件"
            }
        }

    override fun call(): Boolean {

        val isSuccess = if (detectorTasks.any {//如果有一个失败了，就提前退出
                emitStateMessage("处理${it.fullPath}")
                !FileDelete(this, FileInstanceFactory.getFileInstance(it.fullPath, context), context).call()
            }) {
            emitDetailMessage("error", Log.ERROR)
            false
        } else true
        fileOperationProgressListener?.onComplete(
            focused.fullPath, isSuccess, key
        )
        return isSuccess
    }

}