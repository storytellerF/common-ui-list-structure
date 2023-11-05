package com.storyteller_f.giant_explorer.service

import android.content.Context
import android.util.Log
import androidx.core.net.toUri
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.message.Message
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.file_system.model.FileSystemItemModelLite
import com.storyteller_f.file_system.operate.FileDeleteOp
import com.storyteller_f.file_system.operate.FileOperationForemanProgressListener
import com.storyteller_f.file_system.operate.FileOperationListener
import com.storyteller_f.file_system.operate.ScopeFileCopyOp
import com.storyteller_f.file_system.operate.ScopeFileMoveOp
import com.storyteller_f.file_system.operate.ScopeFileMoveOpInShell
import com.storyteller_f.file_system.operate.SuspendCallable
import com.storyteller_f.file_system_ktx.getFileInstance
import com.storyteller_f.multi_core.StoppableTask
import java.io.File

class TaskOverview(val fileCount: Int, val folderCount: Int, val size: Long) {
    val sumCount = fileCount + folderCount
}

abstract class FileOperationForeman(
    val context: Context,
    val overview: TaskOverview,
    val key: String
) : SuspendCallable<Boolean>, StoppableTask, FileOperationListener {
    var fileOperationForemanProgressListener: FileOperationForemanProgressListener? = null
    var leftFileCount = overview.fileCount
    private var leftFolderCount = overview.folderCount
    var leftSize = overview.size
    fun emitCurrentStateMessage() {
        fileOperationForemanProgressListener?.onLeft(leftFileCount, leftFolderCount, leftSize, key)
        fileOperationForemanProgressListener?.onProgress(progress, key)
    }

    protected fun emitDetailMessage(detail: String, level: Int) {
        fileOperationForemanProgressListener?.onDetail(detail, level, key)
    }

    protected fun emitStateMessage(tip: String) {
        fileOperationForemanProgressListener?.onState(tip, key)
    }

    open val progress: Int
        get() {
            val sumCount = overview.sumCount
            val completedCount = sumCount - leftFolderCount - leftFileCount
            return (completedCount * 1.0 / sumCount * 100).toInt()
        }


    override fun onFileDone(fileInstance: FileInstance?, message: Message?, size: Long) {
        leftFileCount--
        leftSize -= size
        emitCurrentStateMessage()
        emitStateMessage("file done ${fileInstance?.name}")
    }

    override fun onDirectoryDone(fileInstance: FileInstance?, message: Message?) {
        leftFolderCount--
        emitCurrentStateMessage()
        emitStateMessage("directory done ${fileInstance?.name}")
    }

    override fun onError(message: Message?) {
        fileOperationForemanProgressListener?.onDetail(
            message?.name + message?.get(),
            Log.ERROR,
            key
        )
    }
}

abstract class LocalFileOperationForeman(
    val focused: FileSystemItemModelLite?, context: Context, overview: TaskOverview, key: String
) : FileOperationForeman(context, overview, key) {

    override fun needStop() = Thread.currentThread().isInterrupted

    abstract val description: String?
}

class CopyForemanImpl(
    private val items: List<FileSystemItemModelLite>,
    private val isMove: Boolean,
    private val target: FileInstance,
    context: Context,
    overview: TaskOverview,
    focused: FileSystemItemModelLite?,
    key: String
) : LocalFileOperationForeman(focused, context, overview, key) {
    override val description: String
        get() {
            val taskName: String
            val fileName: String = items[0].name
            taskName = if (items.size == 1) {
                fileName
            } else {
                fileName + "等" + items.size + "个文件"
            }
            return (if (isMove) "移动" else "复制") + taskName + "到" + target.name
        }

    override suspend fun call(): Boolean {
        val isSuccess = !items.any {
            if (needStop()) {
                emitDetailMessage("已暂停", Log.WARN)
                return false
            }
            val fileInstance = getFileInstance(context, File(it.fullPath).toUri())
            emitStateMessage("处理${fileInstance.path}")
            val operationResult =
                when {
                    !isMove -> ScopeFileCopyOp(fileInstance, target, context).bind(this)
                    fileInstance.javaClass == target.javaClass -> ScopeFileMoveOpInShell(
                        fileInstance,
                        target,
                        context
                    ).bind(this)

                    else -> ScopeFileMoveOp(fileInstance, target, context)
                }.call()
            !operationResult//如果失败了，提前结束
        }
        if (!isSuccess) {
            emitDetailMessage("error in copy impl", Log.ERROR)
        }
        fileOperationForemanProgressListener?.onComplete(
            target.path, isSuccess, key
        )
        return isSuccess
    }

    override val progress: Int
        get() {
            val doneSize: Long = overview.size - leftSize
            return (doneSize * 100.0 / overview.size).toInt()
        }

}

class DeleteForemanImpl(
    private val detectorTasks: List<FileSystemItemModel>,
    context: Context,
    overview: TaskOverview,
    focused: FileSystemItemModel,
    key: String
) : LocalFileOperationForeman(focused, context, overview, key) {

    override val description: String
        get() {
            val firstFile = detectorTasks.first()
            return if (detectorTasks.size == 1) {
                "删除" + firstFile.name
            } else {
                "删除" + firstFile.name + "等" + detectorTasks.size.toString() + "个文件"
            }
        }

    override suspend fun call(): Boolean {
        val isSuccess = !detectorTasks.any {//如果有一个失败了，就提前退出
            emitStateMessage("处理${it.fullPath}")
            !FileDeleteOp(
                getFileInstance(context, File(it.fullPath).toUri()),
                context
            ).apply {
                fileOperationListener = this@DeleteForemanImpl
            }.call()
        }
        fileOperationForemanProgressListener?.onComplete(focused?.fullPath, isSuccess, key)
        return isSuccess
    }

}