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
import com.storyteller_f.file_system.operate.FileCopy
import com.storyteller_f.file_system.operate.FileDelete
import com.storyteller_f.file_system.operate.FileMoveCmd
import com.storyteller_f.file_system.operate.FileOperateListener
import com.storyteller_f.multi_core.StoppableTask
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.source
import java.lang.Exception
import java.nio.ByteBuffer
import java.util.concurrent.Callable

abstract class FileOperateWorker(val context: Context, private val fileCount: Int, private val folderCount: Int, val size: Long, val key: String) : Callable<Boolean>,
    StoppableTask {
    var fileOperationProgressListener: LocalFileOperateWorker.FileOperationProgressListener? = null
    var leftFileCount = fileCount
    var leftFolderCount = folderCount
    var leftSize = size
    fun emitCurrentStateMessage() {
        fileOperationProgressListener?.onLeft(leftFileCount, leftFolderCount, leftSize, key)
        fileOperationProgressListener?.onProgress(progress, key)
    }

    open val progress: Int
        get() {
            val sumCount = fileCount + folderCount
            val completedCount = sumCount - leftFolderCount - leftFileCount
            return (completedCount * 1.0 / sumCount * 100).toInt()
        }

    protected fun emitDetailMessage(detail: String, level: Int) {
        fileOperationProgressListener?.onDetail(detail, level, key)
    }

    protected fun emitStateMessage(tip: String) {
        fileOperationProgressListener?.onState(tip, key)
    }
}

abstract class LocalFileOperateWorker(
    context: Context, fileCount: Int, folderCount: Int, size: Long,
    val focused: FileSystemItemModel, key: String
) : FileOperateWorker(context, fileCount, folderCount, size, key), FileOperateListener {
    constructor(context: Context, focused: FileSystemItemModel, taskEquivalent: TaskEquivalent, key: String) : this(
        context,
        taskEquivalent.fileCount,
        taskEquivalent.folderCount,
        taskEquivalent.size,
        focused,
        key
    )

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


    override fun needStop() = Thread.currentThread().isInterrupted

    abstract val description: String?

    interface FileOperationProgressListener {
        /**
         * ????????????
         *
         * @param progress ????????????
         */
        fun onProgress(progress: Int, key: String)

        /**
         * ??????????????????
         *
         * @param state ??????????????????
         */
        fun onState(state: String?, key: String)

        /**
         * ?????????????????????
         *
         * @param tip ????????????
         */
        fun onTip(tip: String?, key: String)

        /**
         * ???????????????????????????
         */
        fun onDetail(detail: String?, level: Int, key: String)

        /**
         * ??????????????????
         */
        fun onLeft(fileCount: Int, folderCount: Int, size: Long, key: String)

        /**
         * ?????????????????????????????????
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
    context: Context, private val detectorTasks: List<FileSystemItemModel>, taskEquivalent: TaskEquivalent,
    focused: FileSystemItemModel, private val isMove: Boolean, private val dest: FileInstance, key: String
) : LocalFileOperateWorker(context, focused, taskEquivalent, key) {
    override val description: String
        get() {
            val taskName: String
            val fileName: String = detectorTasks[0].name
            taskName = if (detectorTasks.size == 1) {
                fileName
            } else {
                fileName + "???" + detectorTasks.size + "?????????"
            }
            return (if (isMove) "??????" else "??????") + taskName + "???" + dest.name
        }

    override fun call(): Boolean {
        val isSuccess = if (!detectorTasks.any {
                if (needStop()) {
                    emitDetailMessage("?????????", Log.WARN)
                    return false
                }
                val fileInstance = FileInstanceFactory.getFileInstance(it.fullPath, context)
                emitStateMessage("??????${fileInstance.path}")
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
    taskEquivalent: TaskEquivalent,
    focused: FileSystemItemModel, key: String
) : LocalFileOperateWorker(context, focused, taskEquivalent, key) {

    override val description: String
        get() {
            return if (detectorTasks.size == 1) {
                "??????" + focused.name
            } else {
                "??????" + focused.name + "???" + detectorTasks.size.toString() + "?????????"
            }
        }

    override fun call(): Boolean {
        val isSuccess = !detectorTasks.any {//??????????????????????????????????????????
            emitStateMessage("??????${it.fullPath}")
            !FileDelete(this, FileInstanceFactory.getFileInstance(it.fullPath, context), context).call()
        }
        fileOperationProgressListener?.onComplete(focused.fullPath, isSuccess, key)
        return isSuccess
    }

}

class CompoundImpl(private val selected: List<DetectorTask>, private val dest: FileInstance, context: Context, fileCount: Int, folderCount: Int, size: Long, key: String) :
    FileOperateWorker(context, fileCount, folderCount, size, key) {
    override fun call(): Boolean {
        val okHttpClient = OkHttpClient()
        val isSuccess = !selected.any { //have error?
            try {
                when (it) {
                    is DownloadTask -> !executeDownload(okHttpClient, it)
                    is LocalTask -> !FileCopy(this, FileInstanceFactory.getFileInstance(it.path, context), dest, context).call()
                    is ContentTask -> !executeContentTask(it)
                    else -> {
                        emitDetailMessage("?????????????????????${it.javaClass}", Log.ASSERT)
                        true
                    }
                }
            } catch (e: Exception) {
                emitDetailMessage(e.exceptionMessage, Log.ERROR)
                true
            }
        }
        fileOperationProgressListener?.onComplete(null, isSuccess, key)
        return isSuccess
    }

    private fun executeContentTask(contentTask: ContentTask): Boolean {
        try {
            val contentResolver = context.contentResolver
            val query = contentResolver.query(contentTask.uri, null, null, null, null) ?: kotlin.run {
                emitDetailMessage("${contentTask.uri} ????????????", Log.ERROR)
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