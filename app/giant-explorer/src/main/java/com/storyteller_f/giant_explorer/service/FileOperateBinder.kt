package com.storyteller_f.giant_explorer.service

import android.content.Context
import android.os.Binder
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import com.storyteller_f.file_system.FileInstanceFactory.getFileInstance
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.giant_explorer.service.FileOperateService.FileOperateResult
import java.util.*
import kotlin.concurrent.thread

class FileOperateBinder(val context: Context) : Binder() {
    var fileOperationProgressListener = mutableMapOf<String, MutableList<FileOperateWorker.FileOperationProgressListener>>()
    val map = mutableMapOf<String, Task>()
    private val progressListenerLocal = object : FileOperateWorker.FileOperationProgressListener {
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
    private var fileOperateResult: FileOperateResult? = null
    fun setFileOperateResult(fileOperateResult: FileOperateResult?) {
        this.fileOperateResult = fileOperateResult
    }

    /**
     * 删除文件或者文件夹
     * @param selected  要删除的多个文件
     * @param focused     内存卡根部tree fileSystemItemModel
     */
    fun delete(focused: FileSystemItemModel, selected: List<FileSystemItemModel>, key: String) {
        thread {
            startDeleteTask(focused, selected, key)
        }
    }

    private fun preTask(selected: List<FileSystemItemModel?>): LinkedList<DetectorTask>? {
        state.postValue(state_detect)
        val multiDetector = MultiDetector(selected)
        val detectorTasks = multiDetector.start(context)
        val filterIsInstance = detectorTasks.filterIsInstance<ErrorTask>()
        if (filterIsInstance.isNotEmpty()) {
            fileOperateResult?.onError(filterIsInstance.first().message)
            return null
        }
        return detectorTasks
    }

    @WorkerThread
    private fun startDeleteTask(focused: FileSystemItemModel, selected: List<FileSystemItemModel>, key: String) {
        state.postValue(state_compute)
        val compute = TaskCompute(selected, context).compute()
        if (compute == null) {
            fileOperateResult?.onError("出现错误")
            return
        }
        Log.i(TAG, "startDeleteTask: compute size ${compute.size}")
        state.postValue(state_running)
        if (DeleteImpl(context, selected, compute, focused, key).let {
                it.fileOperationProgressListener = progressListenerLocal
                it.call()
            }) {
            state.postValue(state_end)
            fileOperateResult?.onSuccess(null, focused.fullPath)
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
        dest: FileInstance,
        focused: FileSystemItemModel,
        deleteOrigin: Boolean,
        selected: List<FileSystemItemModel>,
        uuid: String
    ) {
        state.postValue(state_compute)
        val computeSize = TaskCompute(selected, context).compute()
        if (computeSize == null) {
            fileOperateResult?.onError("error")
            return
        }
        Log.i(TAG, "startCopyTask: compute size ${computeSize.size}")
        state.postValue(state_running)
        map[uuid] = computeSize
        if (CopyImpl(context, selected, computeSize, focused, deleteOrigin, dest, uuid).let {
                it.fileOperationProgressListener = progressListenerLocal
                it.call()
            }) {
            state.postValue(state_end)
            fileOperateResult?.onSuccess(dest.path, focused.fullPath)
        }
    }


    fun moveOrCopy(dest: FileInstance, selected: List<FileSystemItemModel>, focused: FileSystemItemModel, deleteOrigin: Boolean, uuid: String) {
        thread {
            startCopyTask(dest, focused, deleteOrigin, selected, uuid)
        }
    }

    companion object {
        private const val TAG = "FileOperateHandler"

        const val state_null = 0
        const val state_detect = 1
        const val state_compute = 2
        const val state_running = 3
        const val state_end = 4
    }
}

class Task(val fileCount: Int, val folderCount: Int, val size: Long)

class TaskCompute(private val detectorTasks: List<FileSystemItemModel>, val context: Context) {
    var count = 0
    var folderCount = 0
    fun compute(): Task? {
        val size = detectorTasks.map {
            val fileInstance = getFileInstance(it.fullPath, context)
            if (!fileInstance.exists()) {
                return null
            }
            if (it is FileItemModel) {
                count++
                getFileInstance(it.fullPath, context).fileLength
            } else {
                getDirectorySize(it)
            }
        }.reduce { acc, l -> acc + l }
        return Task(count, folderCount, size)
    }

    private fun getDirectorySize(file: FileSystemItemModel): Long {
        folderCount++
        val fileInstance = getFileInstance(file.fullPath, context)
        val listSafe = fileInstance.listSafe()

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