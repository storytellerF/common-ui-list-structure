package com.storyteller_f.giant_explorer.service

import android.content.Context
import android.util.Log
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import com.storyteller_f.common_ktx.exceptionMessage
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.message.Message
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.file_system.operate.*
import com.storyteller_f.multi_core.StoppableTask
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.source
import java.nio.ByteBuffer
import java.util.concurrent.Callable

class TaskOverview(val fileCount: Int, val folderCount: Int, val size: Long) {
    val sumCount = fileCount + folderCount
}

abstract class FileOperationForeman(val context: Context, val overview: TaskOverview, val key: String) : Callable<Boolean>, StoppableTask, FileOperationListener {
    var fileOperationForemanProgressListener: FileOperationForemanProgressListener? = null
    var leftFileCount = overview.fileCount
    var leftFolderCount = overview.folderCount
    var leftSize = overview.size
    fun emitCurrentStateMessage() {
        fileOperationForemanProgressListener?.onLeft(leftFileCount, leftFolderCount, leftSize, key)
        fileOperationForemanProgressListener?.onProgress(progress, key)
    }

    open val progress: Int
        get() {
            val sumCount = overview.sumCount
            val completedCount = sumCount - leftFolderCount - leftFileCount
            return (completedCount * 1.0 / sumCount * 100).toInt()
        }

    protected fun emitDetailMessage(detail: String, level: Int) {
        fileOperationForemanProgressListener?.onDetail(detail, level, key)
    }

    protected fun emitStateMessage(tip: String) {
        fileOperationForemanProgressListener?.onState(tip, key)
    }

    override fun onFileDone(fileInstance: FileInstance?, message: Message?, size: Long, type: Int) {
        leftFileCount--
        leftSize -= size
        emitCurrentStateMessage()
        emitStateMessage("file done ${fileInstance?.name}")
    }

    override fun onDirectoryDone(fileInstance: FileInstance?, message: Message?, type: Int) {
        leftFolderCount--
        emitCurrentStateMessage()
        emitStateMessage("directory done ${fileInstance?.name}")
    }

    override fun onError(message: Message?, type: Int) {
        fileOperationForemanProgressListener?.onDetail(message?.name + message?.get(), Log.ERROR, key)
    }
}

abstract class LocalFileOperationForeman(
    val focused: FileSystemItemModel, context: Context, overview: TaskOverview, key: String
) : FileOperationForeman(context, overview, key) {

    override fun needStop() = Thread.currentThread().isInterrupted

    abstract val description: String?
}

class CopyForemanImpl(
    private val detectorTasks: List<FileSystemItemModel>,
    private val isMove: Boolean,
    private val dest: FileInstance,
    context: Context,
    overview: TaskOverview,
    focused: FileSystemItemModel,
    key: String
) : LocalFileOperationForeman(focused, context, overview, key) {
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
                if (isMove) !FileMoveOpInShell(this, fileInstance, dest, context).apply {
                    fileOperationListener = this@CopyForemanImpl
                }.call()
                else !FileCopyOp(this, fileInstance, dest, context).apply {
                    fileOperationListener = this@CopyForemanImpl
                }.call()
            }) {
            emitDetailMessage("error", Log.ERROR)
            true
        } else false
        fileOperationForemanProgressListener?.onComplete(
            dest.path, isSuccess, key
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
    private val detectorTasks: List<FileSystemItemModel>, context: Context, overview: TaskOverview, focused: FileSystemItemModel, key: String
) : LocalFileOperationForeman(focused, context, overview, key) {

    override val description: String
        get() {
            return if (detectorTasks.size == 1) {
                "删除" + focused.name
            } else {
                "删除" + focused.name + "等" + detectorTasks.size.toString() + "个文件"
            }
        }

    override fun call(): Boolean {
        val isSuccess = !detectorTasks.any {//如果有一个失败了，就提前退出
            emitStateMessage("处理${it.fullPath}")
            !FileDeleteOp(this, FileInstanceFactory.getFileInstance(it.fullPath, context), context).apply {
                fileOperationListener = this@DeleteForemanImpl
            }.call()
        }
        fileOperationForemanProgressListener?.onComplete(focused.fullPath, isSuccess, key)
        return isSuccess
    }

}

class CompoundForemanImpl(private val selected: List<DetectedTask>, private val dest: FileInstance, context: Context, overview: TaskOverview, key: String) : FileOperationForeman(context, overview, key), FileOperationListener {
    override fun call(): Boolean {
        val okHttpClient = OkHttpClient()
        val isSuccess = !selected.any { //have error?
            try {
                when (it) {
                    is DownloadTask -> !executeDownload(okHttpClient, it)
                    is LocalTask -> !FileCopyOp(this, FileInstanceFactory.getFileInstance(it.path, context), dest, context).apply {
                        fileOperationListener = this@CompoundForemanImpl
                    }.call()
                    is ContentTask -> !executeContentTask(it)
                    else -> {
                        emitDetailMessage("无法识别的任务${it.javaClass}", Log.ASSERT)
                        true
                    }
                }
            } catch (e: Exception) {
                emitDetailMessage(e.stackTraceToString(), Log.ERROR)
                true
            }
        }
        fileOperationForemanProgressListener?.onComplete(null, isSuccess, key)
        return isSuccess
    }

    private fun executeContentTask(contentTask: ContentTask): Boolean {
        try {
            val contentResolver = context.contentResolver
            val query = contentResolver.query(contentTask.uri, null, null, null, null) ?: kotlin.run {
                emitDetailMessage("${contentTask.uri} 解析失败", Log.ERROR)
                return false
            }
            val name = query.use {
                (if (query.moveToFirst()) {
                    val columnIndex = query.getColumnIndex("name")
                    if (columnIndex >= 0) {
                        query.getString(columnIndex)
                    } else null
                } else null) ?: kotlin.run {
                    "${System.currentTimeMillis()}.${MimeTypeMap.getFileExtensionFromUrl(contentTask.uri.path)}"
                }
            }

            contentResolver.openInputStream(contentTask.uri)?.source()?.buffer()?.use { int ->
                dest.toChild(name, true, true).fileOutputStream.channel.use { out ->
                    val byteBuffer = ByteBuffer.allocateDirect(1024)
                    while (int.read(byteBuffer) != -1) {
                        if (needStop()) return false
                        byteBuffer.flip()
                        out.write(byteBuffer)
                        byteBuffer.clear()
                    }
                    leftFileCount--
                    emitCurrentStateMessage()
                    return true
                }
            }
            return false
        } catch (e: Exception) {
            emitDetailMessage("${contentTask.uri}:-:${e.exceptionMessage}", Log.ASSERT)
            return false
        }

    }

    private fun executeDownload(okHttpClient: OkHttpClient, it: DownloadTask): Boolean {
        try {
            val execute = okHttpClient.newCall(Request.Builder().url(it.url).build()).execute()
            if (execute.isSuccessful) {
                val contentDisposition = execute.header("Content-Disposition")
                val contentType = execute.header("content-type")
                Log.i(TAG, "call: $contentDisposition")
                val guessFileName = URLUtil.guessFileName(it.url, contentDisposition, contentType)
                (execute.body?.source()?.buffer ?: return true).use { int ->
                    dest.toChild(guessFileName, true, true).fileOutputStream.channel.use { out ->
                        val byteBuffer = ByteBuffer.allocateDirect(1024)
                        while (int.read(byteBuffer) != -1) {
                            if (needStop()) return false
                            byteBuffer.flip()
                            out.write(byteBuffer)
                            byteBuffer.clear()
                        }
                        leftFileCount--
                        emitCurrentStateMessage()
                        return true
                    }
                }
            } else {
                emitDetailMessage("${it.url} code is ${execute.code}", Log.ERROR)
            }
            return false
        } catch (e: Exception) {
            emitDetailMessage("${it.url}:-:${e.exceptionMessage}", Log.ERROR)
            return false
        }

    }

    override fun needStop() = Thread.currentThread().isInterrupted

    companion object {
        private const val TAG = "LocalFileOperateWorker"
    }
}