package com.storyteller_f.giant_explorer.service

import android.content.Context
import android.os.Binder
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.giant_explorer.service.FileOperateService.FileOperateResult
import java.util.*
import kotlin.concurrent.thread

class FileOperateBinder(val context: Context) : Binder() {
    var fileOperationProgressListener: FileOperateWorker.FileOperationProgressListener? = null
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
    fun delete(focused: FileSystemItemModel, selected: List<FileSystemItemModel?>) {
        thread {
            val detectorTasks = preTask(selected)
            detectorTasks?.let {
                startDeleteTask(focused, detectorTasks)
            }
            fileOperateResult?.onSuccess(null, focused.fullPath)
        }

    }

    private fun preTask(selected: List<FileSystemItemModel?>): LinkedList<DetectorTask>? {
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
    private fun startDeleteTask(focused: FileSystemItemModel, detectorTasks: LinkedList<DetectorTask>) {
        state.postValue(state_compute)
        val compute = TaskCompute(detectorTasks, context).compute()
        Log.i(TAG, "startDeleteTask: compute size ${compute.size}")
        state.postValue(state_running)
        DeleteImpl(context, detectorTasks, compute, focused).let {
            it.fileOperationProgressListener = fileOperationProgressListener
            it.run()
        }
        state.postValue(state_end)
        fileOperateResult?.onSuccess(null, focused.fullPath)
    }

    /**
     * 启动复制任务
     *
     * @param dest          复制到
     * @param focused          被复制的路径
     * @param detectorTasks 分配好的任务
     */
    @WorkerThread
    private fun startCopyTask(dest: FileInstance, focused: FileSystemItemModel, deleteOrigin: Boolean, detectorTasks: List<DetectorTask>) {
        state.postValue(state_compute)
        val computeSize = TaskCompute(detectorTasks, context).compute()
        Log.i(TAG, "startCopyTask: compute size ${computeSize.size}")
        state.postValue(state_running)
        CopyImpl(context, detectorTasks, computeSize, focused, deleteOrigin, dest).let {
            it.fileOperationProgressListener = fileOperationProgressListener
            it.run()
        }
        state.postValue(state_end)
        fileOperateResult?.onSuccess(dest.path, focused.fullPath)
    }


    fun moveOrCopy(dest: FileInstance, selected: List<FileSystemItemModel>, focused: FileSystemItemModel, deleteOrigin: Boolean) {
        state.value = state_detect
        thread {
            val detectorTasks = preTask(selected)
            detectorTasks?.let {
                startCopyTask(dest, focused, deleteOrigin, detectorTasks)
            }
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

class TaskCompute(private val detectorTasks: List<DetectorTask>, val context: Context) {
    var count = 0
    var folderCount = 0
    fun compute(): Task {
        return Task(count, folderCount, detectorTasks.map {
            if (it is ValidTask) {
                when (it.type) {
                    ValidTask.type_empty -> {
                        folderCount++
                        0
                    }
                    ValidTask.type_file -> {
                        count++
                        FileInstanceFactory.getFileInstance(it.file.fullPath, context).fileLength
                    }
                    ValidTask.type_not_empty -> {
                        getDirectorySize(it.file)
                    }
                    else -> 0
                }
            } else 0
        }.reduce { acc, l -> acc + l })

    }

    private fun getDirectorySize(file: FileSystemItemModel): Long {
        folderCount++
        val fileInstance = FileInstanceFactory.getFileInstance(file.fullPath, context)
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