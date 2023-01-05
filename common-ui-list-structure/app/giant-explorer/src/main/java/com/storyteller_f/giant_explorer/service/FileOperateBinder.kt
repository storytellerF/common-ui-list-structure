package com.storyteller_f.giant_explorer.service

import android.content.Context
import android.net.Uri
import android.os.Binder
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import com.storyteller_f.common_ktx.exceptionMessage
import com.storyteller_f.file_system.FileInstanceFactory.getFileInstance
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.file_system.operate.FileOperationForemanProgressListener
import com.storyteller_f.giant_explorer.service.FileOperateService.FileOperateResultContainer
import com.storyteller_f.plugin_core.GiantExplorerService
import okio.FileNotFoundException
import java.lang.ref.WeakReference
import java.util.*
import kotlin.concurrent.thread

class FileOperateBinder(val context: Context) : Binder() {
    var fileOperationProgressListener = mutableMapOf<String, MutableList<FileOperationForemanProgressListener>>()
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
            fileOperationProgressListener[key]?.forEach { it.onLeft(fileCount, folderCount, size, key) }
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
            startDeleteTask(focused, selected, key)
        }
    }

    fun moveOrCopy(dest: FileInstance, selected: List<FileSystemItemModel>, focused: FileSystemItemModel, deleteOrigin: Boolean, key: String) {
        whenStart(key)
        thread {
            startCopyTask(dest, focused, deleteOrigin, selected, key)
        }
    }

    fun compoundTask(selected: List<Uri>, dest: FileInstance, key: String) {
        whenStart(key)
        thread {
            preTask(selected, key)?.let { linkedList ->
                startCompoundTask(linkedList, dest, key)
            }
        }
    }

    fun pluginTask(key: String, block: GiantExplorerService.() -> Boolean) {
        whenStart(key)
        thread {
            val value = object : GiantExplorerService {
                override fun reportRunning() {
                    taskStarted(key, TaskAssessResult(0, 0, 0))
                }
            }
            if (block.invoke(value)) {
                whenEnd(key)
            }
        }
    }

    @WorkerThread
    private fun startDeleteTask(focused: FileSystemItemModel, selected: List<FileSystemItemModel>, key: String) {
        state.postValue(state_computing)
        val assessResult = TaskAssessor(selected, context, null).assess()
        state.postValue(state_running)
        val deleteForemanImpl = DeleteForemanImpl(selected, context, assessResult.toOverview(), focused, key).attachListener()
        if (deleteForemanImpl.call()) {
            whenEnd(key)
            fileOperateResultContainer.get()?.onSuccess(null, focused.fullPath)
        }
    }

    /**
     * 启动复制任务
     *
     * @param dest          复制到
     * @param focused          被复制的路径
     * @param selected 分配好的任务
     */
    @WorkerThread
    private fun startCopyTask(
        dest: FileInstance, focused: FileSystemItemModel, deleteOrigin: Boolean, selected: List<FileSystemItemModel>, key: String
    ) {
        state.postValue(state_computing)
        val assessResult = TaskAssessor(selected, context, dest).assess()
        taskStarted(key, assessResult)
        val copyForemanImpl = CopyForemanImpl(selected, deleteOrigin, dest, context, assessResult.toOverview(), focused, key).attachListener()
        if (copyForemanImpl.call()) {
            whenEnd(key)
            fileOperateResultContainer.get()?.onSuccess(dest.path, focused.fullPath)
        }
    }

    @WorkerThread
    private fun startCompoundTask(linkedList: LinkedList<DetectedTask>, dest: FileInstance, key: String) {
        state.postValue(state_computing)
        val filterIsInstance = linkedList.filterIsInstance<LocalTask>().map {
            getFileInstance(it.path, context).fileSystemItem
        }
        val count = linkedList.filter {
            it is ContentTask || it is DownloadTask
        }.size
        val assessResult = if (filterIsInstance.isNotEmpty()) {
            try {
                TaskAssessor(filterIsInstance, context, dest).assess()
            } catch (e: Exception) {
                whenError(key, e.exceptionMessage)
                return
            }
        } else TaskAssessResult.empty
        taskStarted(key, assessResult)
        val compoundForemanImpl = CompoundForemanImpl(linkedList, dest, context, assessResult.toOverview(count), key).attachListener()
        if (compoundForemanImpl.call()) {
            whenEnd(key)
            fileOperateResultContainer.get()?.onSuccess(dest.path, null)
        }
    }

    private fun preTask(selected: List<Uri>, key: String): LinkedList<DetectedTask>? {
        state.postValue(state_detect)
        val detectorTasks = MultiDetector(selected).start()
        if (detectorTasks.isEmpty()) {
            whenError(key, "包含非法任务")
            return null
        }
        val hasErrorTask = detectorTasks.filterIsInstance<ErrorTask>()
        if (hasErrorTask.isNotEmpty()) {
            Log.i(TAG, "preTask: error ${hasErrorTask.joinToString(",") { it.message }}")
            whenError(key, hasErrorTask.first().message)
            return null
        }
        return detectorTasks
    }

    private fun taskStarted(key: String, computeSize: TaskAssessResult) {
        map[key] = TaskSession(computeSize, null)
        state.postValue(state_running)
    }

    private fun whenStart(key: String) {
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

        const val state_null = 0
        const val state_detect = 1
        const val state_computing = 2
        const val state_running = 3
        const val state_end = 4
        const val state_error = 5

        fun checkOperationValid(path: String, dest: String): Boolean {
            return dest.contains(path)
        }
    }
}

class TaskSession(val taskAssessResult: TaskAssessResult?, val message: CharSequence?)

class TaskAssessResult(val fileCount: Int, val folderCount: Int, val size: Long) {
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
class TaskAssessor(private val detectorTasks: List<FileSystemItemModel>, val context: Context, private val dest: FileInstance?) {
    private var count = 0
    private var folderCount = 0
    fun assess(): TaskAssessResult {
        val size = detectorTasks.map {
            if (dest != null && FileOperateBinder.checkOperationValid(it.fullPath, dest.path)) throw Exception("不能将父文件夹移动到子文件夹")
            val fileInstance = getFileInstance(it.fullPath, context)
            if (!fileInstance.exists()) {
                throw FileNotFoundException(fileInstance.path)
            }
            if (it is FileItemModel) {
                count++
                getFileInstance(it.fullPath, context).fileLength
            } else {
                getDirectorySize(it)
            }
        }.plus(0).reduce { acc, l -> acc + l }
        if (count.toLong() + folderCount.toLong() + size == 0L) throw Exception("无合法任务")
        return TaskAssessResult(count, folderCount, size)
    }

    private fun getDirectorySize(file: FileSystemItemModel): Long {
        folderCount++
        val fileInstance = getFileInstance(file.fullPath, context)
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