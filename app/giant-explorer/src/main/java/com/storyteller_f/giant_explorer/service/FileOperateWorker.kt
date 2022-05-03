package com.storyteller_f.giant_explorer.service

import android.content.Context
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.message.Message
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.file_system.operate.FileCopy
import com.storyteller_f.file_system.operate.FileDelete
import com.storyteller_f.file_system.operate.FileMoveCmd
import com.storyteller_f.file_system.operate.FileOperateListener
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.multi_core.StoppableTask

abstract class FileOperateWorker internal constructor(
    val context: Context, private val fileCount: Int, private val folderCount: Int, val size: Long,
    val focused: FileSystemItemModel
) : Runnable, FileOperateListener, StoppableTask {
    constructor(context: Context, focused: FileSystemItemModel, task: Task):this(context, task.fileCount, task.folderCount, task.size, focused)
    var fileOperationProgressListener: FileOperationProgressListener? = null
    var leftFileCount = fileCount
    var leftFolderCount = folderCount
    var leftSize = size
    fun emitCurrentStateMessage() {
        fileOperationProgressListener?.onLeft(leftFileCount, leftFolderCount, leftSize)
        fileOperationProgressListener?.onProgress(progress)
    }

    override fun onFileDone(fileInstance: FileInstance?, type: Int, message: Message?) {
        leftFileCount--
        leftSize -= size
        emitCurrentStateMessage()
    }

    override fun onDirectoryDone(fileInstance: FileInstance?, type: Int, message: Message?) {
        leftFolderCount--
        emitCurrentStateMessage()
    }

    override fun onError(message: Message?, type: Int) {
        fileOperationProgressListener?.onDetail(message?.name + message?.get(), R.color.color_failed)
    }


    override fun needStop(): Boolean {
        return Thread.currentThread().isInterrupted
    }

    abstract val description: String?

    abstract override fun run()

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
        fun onProgress(progress: Int)

        /**
         * 正在做的工作
         *
         * @param state 新的状态信息
         */
        fun onState(state: String?)

        /**
         * 进入某个文件夹
         *
         * @param tip 新的提示
         */
        fun onTip(tip: String?)

        /**
         * 需要展示的详细信息
         */
        fun onDetail(detail: String?, color: Int)

        /**
         * 还剩余的任务
         */
        fun onLeft(file_count: Int, folder_count: Int, size: Long)

        /**
         * 任务完成，可以刷新页面
         *
         * @param dest
         */
        fun onComplete(dest: String?)
    }

}

class CopyImpl(
    context: Context, private val detectorTasks: List<DetectorTask>, task: Task,
    focused: FileSystemItemModel, private val isMove: Boolean, private val dest: FileInstance
) : FileOperateWorker(context, focused, task) {
    override val description: String
        get() {
            val taskName: String
            val fileName: String = detectorTasks[0].file.name
            taskName = if (detectorTasks.size == 1) {
                fileName
            } else {
                fileName + "等" + detectorTasks.size + "个文件"
            }
            return (if (isMove) "移动" else "复制") + taskName + "到" + dest.name
        }

    override fun run() {
        val any = detectorTasks.any {
            if (needStop()) return
            val file = it.file
            val fileInstance = FileInstanceFactory.getFileInstance(file.fullPath, context)
            if (isMove)
                !FileMoveCmd(this, fileInstance, dest, context).apply {
                    fileOperateListener = this@CopyImpl
                }.call()
            else
                !FileCopy(this, fileInstance, dest, context).apply {
                    fileOperateListener = this@CopyImpl
                }.call()
        }
        if (!any) {
            fileOperationProgressListener?.onComplete(dest.path)
        }
    }

    override val progress: Int
        get() {
            val doneSize: Long = size - leftSize
            return (doneSize * 100.0 / size).toInt()
        }

}

class DeleteImpl(
    context: Context, private val detectorTasks: List<DetectorTask>,
    task: Task,
    focused: FileSystemItemModel
) : FileOperateWorker(context, focused, task) {

    override val description: String
        get() {
            return if (detectorTasks.size == 1) {
                "删除" + focused.name
            } else {
                "删除" + focused.name + "等" + detectorTasks.size.toString() + "个文件"
            }
        }

    override fun run() {
        if (detectorTasks.any {//如果有一个失败了，就提前退出
                !FileDelete(this, FileInstanceFactory.getFileInstance(it.file.fullPath, context), context).call()
            }) {
            fileOperationProgressListener?.onDetail("error", 0)
        } else {
            fileOperationProgressListener?.onComplete(focused.fullPath)
        }
    }

}