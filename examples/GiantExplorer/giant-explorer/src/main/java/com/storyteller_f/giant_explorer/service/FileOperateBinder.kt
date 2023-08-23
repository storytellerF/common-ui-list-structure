package com.storyteller_f.giant_explorer.service

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Binder
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.file_system.model.FileSystemItemModelLite
import com.storyteller_f.file_system.operate.FileOperationForemanProgressListener
import com.storyteller_f.file_system_ktx.getFileInstance
import com.storyteller_f.giant_explorer.service.FileOperateService.FileOperateResultContainer
import com.storyteller_f.plugin_core.GiantExplorerService
import kotlinx.coroutines.runBlocking
import okio.FileNotFoundException
import java.io.File
import java.lang.ref.WeakReference
import kotlin.concurrent.thread

class FileOperateBinder(val context: Context) : Binder() {
    var fileOperationProgressListener =
        mutableMapOf<String, MutableList<FileOperationForemanProgressListener>>()
    val map = mutableMapOf<String, TaskSession>()
    private val progressListenerLocal = object : FileOperationForemanProgressListener {
        override fun onProgress(progress: Int, key: String) {
            fileOperationProgressListener[key]?.forEach { it.onProgress(progress, key) }
        }

        override fun onState(state: String?, key: String) {
            fileOperationProgressListener[key]?.forEach { it.onState(state, key) }
        }

        override fun onTip(tip: String?, key: String) {
            fileOperationProgressListener[key]?.forEach { it.onTip(tip, key) }
        }

        override fun onDetail(detail: String?, level: Int, key: String) {
            fileOperationProgressListener[key]?.forEach { it.onDetail(detail, level, key) }
        }

        override fun onLeft(fileCount: Int, folderCount: Int, size: Long, key: String) {
            fileOperationProgressListener[key]?.forEach {
                it.onLeft(
                    fileCount, folderCount, size, key
                )
            }
        }

        override fun onComplete(dest: String?, isSuccess: Boolean, key: String) {
            fileOperationProgressListener[key]?.forEach { it.onComplete(dest, isSuccess, key) }
        }

    }
    val state = MutableLiveData(state_null)
    var fileOperateResultContainer: WeakReference<FileOperateResultContainer> = WeakReference(null)

    /**
     * 删除文件或者文件夹
     * @param selected  要删除的多个文件
     * @param focused     内存卡根部tree fileSystemItemModel
     */
    fun delete(focused: FileSystemItemModel, selected: List<FileSystemItemModel>, key: String) {
        whenStart(key)
        thread {
            runBlocking {
                startDeleteTask(focused, selected, key)
            }
        }
    }

    fun moveOrCopy(
        dest: FileInstance,
        selected: List<FileSystemItemModelLite>,
        focused: FileSystemItemModel?,
        deleteOrigin: Boolean,
        key: String
    ) {
        whenStart(key)
        thread {
            runBlocking {
                startCopyTask(dest, focused, deleteOrigin, selected, key)
            }
        }
    }

    fun pluginTask(key: String, block: suspend GiantExplorerService.() -> Boolean) {
        whenStart(key)
        thread {
            val value = object : GiantExplorerService {
                override fun reportRunning() {
                    whenRunning(key, TaskAssessResult(0, 0, 0))
                }
            }
            runBlocking {
                if (block.invoke(value)) {
                    whenEnd(key)
                }
            }
        }
    }

    @WorkerThread
    private suspend fun startDeleteTask(
        focused: FileSystemItemModel, selected: List<FileSystemItemModel>, key: String
    ) {
        state.postValue(state_computing)
        val assessResult = runBlocking {
            TaskAssessor(selected, context, null).assess()
        }
        state.postValue(state_running)
        val deleteForemanImpl = DeleteForemanImpl(
            selected, context, assessResult.toOverview(), focused, key
        ).attachListener()
        if (deleteForemanImpl.call()) {
            whenEnd(key)
            fileOperateResultContainer.get()?.onSuccess(null, focused.uri)
        }
    }

    /**
     * 启动复制任务。跳过detect 阶段
     *
     * @param dest          复制到
     * @param focused       被复制的路径
     * @param selected      分配好的任务
     */
    @WorkerThread
    private suspend fun startCopyTask(
        dest: FileInstance,
        focused: FileSystemItemModelLite?,
        deleteOrigin: Boolean,
        selected: List<FileSystemItemModelLite>,
        key: String
    ) {
        state.postValue(state_computing)
        val assessResult = runBlocking {
            TaskAssessor(selected, context, dest).assess()
        }
        whenRunning(key, assessResult)
        val copyForemanImpl = CopyForemanImpl(
            selected, deleteOrigin, dest, context, assessResult.toOverview(), focused, key
        ).attachListener()
        if (copyForemanImpl.call()) {
            whenEnd(key)
            fileOperateResultContainer.get()?.onSuccess(dest.uri, focused?.uri)
        }
    }

    private fun detectUri(selected: List<Uri>, key: String): List<Uri>? {
        state.postValue(state_detect)
        val supportTasks = selected.filter {
            supportUri.contains(it.scheme)
        }
        if (supportTasks.isEmpty()) {
            whenError(key, "没有合法任务")
            return null
        }
        val errorTasks = selected - supportTasks.toSet()
        if (errorTasks.isNotEmpty()) {
            whenError(key, "unrecognized uri: ${errorTasks.joinToString()}")
            return null
        }
        return supportTasks
    }

    private fun whenRunning(key: String, computeSize: TaskAssessResult) {
        Log.d(TAG, "whenRunning() called with: key = $key, computeSize = $computeSize")
        map[key] = TaskSession(computeSize, null)
        state.postValue(state_running)
    }

    private fun whenStart(key: String) {
        Log.d(TAG, "whenStart() called with: key = $key")
        map.getOrPut(key) {
            TaskSession(null, null)
        }
        state.value = state_null
    }

    private fun whenEnd(key: String) {
        Log.d(TAG, "whenEnd() called with: key = $key")
        state.postValue(state_end)
        map.remove(key)
    }

    private fun whenError(key: String, message: String) {
        Log.d(TAG, "whenError() called with: key = $key, message = $message")
        map[key] = TaskSession(null, message)
        state.postValue(state_error)
        fileOperateResultContainer.get()?.onError(message)
    }

    private fun FileOperationForeman.attachListener(): FileOperationForeman {
        fileOperationForemanProgressListener = progressListenerLocal
        return this
    }

    companion object {
        private const val TAG = "FileOperateHandler"

        /**
         * 任务已重置
         */
        const val state_null = 0

        /**
         * 解析任务列表
         */
        const val state_detect = 1

        /**
         * 计算大小
         */
        const val state_computing = 2

        /**
         * 任务正在运行
         */
        const val state_running = 3

        /**
         * 任务正常结束
         */
        const val state_end = 4

        /**
         * 任务非正常结束
         */
        const val state_error = 5

        val supportUri = listOf(
            ContentResolver.SCHEME_CONTENT,
            ContentResolver.SCHEME_FILE,
            "http",
            "https",
            "ftp",
            "ftpes",
            "ftps",
            "sftp",
            "smb",
            "webdav"
        )

        fun checkOperationValid(path: String, dest: String): Boolean {
            return dest.contains(path)
        }
    }
}

class TaskSession(val taskAssessResult: TaskAssessResult?, val message: CharSequence?)

data class TaskAssessResult(val fileCount: Int, val folderCount: Int, val size: Long) {
    fun toOverview(fileCountExtra: Int = 0): TaskOverview {
        return TaskOverview(fileCount + fileCountExtra, folderCount, size)
    }

    companion object {
        val empty = TaskAssessResult(0, 0, 0)
    }
}

/**
 * 任务评估。
 */
class TaskAssessor(
    private val detectorTasks: List<FileSystemItemModelLite>,
    val context: Context,
    private val dest: FileInstance?
) {
    private var count = 0
    private var folderCount = 0
    suspend fun assess(): TaskAssessResult {
        val size = detectorTasks.map {
            if (dest != null && FileOperateBinder.checkOperationValid(
                    it.fullPath, dest.path
                )
            ) throw Exception("不能将父文件夹移动到子文件夹")
            val fileInstance = getFileInstance(context, File(it.fullPath).toUri())
            if (!fileInstance.exists()) {
                throw FileNotFoundException(fileInstance.path)
            }
            if (it is FileItemModel) {
                count++
                getFileInstance(context, File(it.fullPath).toUri()).getFileLength()
            } else {
                getDirectorySize(it)
            }
        }.plus(0).reduce { acc, l -> acc + l }
        if (count.toLong() + folderCount.toLong() + size == 0L) throw Exception("无合法任务")
        return TaskAssessResult(count, folderCount, size)
    }

    private suspend fun getDirectorySize(file: FileSystemItemModelLite): Long {
        folderCount++
        val fileInstance = getFileInstance(context, File(file.fullPath).toUri())
        val listSafe = fileInstance.list()

        val fileSize = listSafe.files.map {
            count++
            it.size
        }.plus(0).reduce { acc, l -> acc + l }
        val directorySize = listSafe.directories.map {
            getDirectorySize(it)
        }.plus(0).reduce { acc, l -> acc + l }
        return fileSize + directorySize
    }

}