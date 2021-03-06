package com.storyteller_f.giant_explorer

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.work.*
import com.osama.firecrasher.CrashListener
import com.osama.firecrasher.FireCrasher
import com.storyteller_f.common_ktx.exceptionMessage
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.checkPathPermission
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.TorrentFileModel
import com.storyteller_f.giant_explorer.control.adapter_produce.Temp
import com.storyteller_f.giant_explorer.database.FileMDRecord
import com.storyteller_f.giant_explorer.database.FileSizeRecord
import com.storyteller_f.giant_explorer.database.FileTorrentRecord
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.utils.TorrentFile
import com.storyteller_f.multi_core.StoppableTask
import kotlinx.coroutines.*
import java.math.BigInteger
import java.security.MessageDigest

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Temp.add()
        MainScope().launch {
            requireDatabase.bigTimeDao().fetchSuspend().groupBy {
                it.workerName
            }.forEach { entry ->
                WorkManager.getInstance(this@App).enqueueUniqueWork(
                    entry.key,
                    ExistingWorkPolicy.KEEP,
                    when (entry.key) {
                        "message digest" -> OneTimeWorkRequestBuilder<MDWorker>()
                        "folder size" -> OneTimeWorkRequestBuilder<FolderWorker>()
                        else -> OneTimeWorkRequestBuilder<TorrentWorker>()
                    }.setInputData(
                        Data.Builder().putStringArray("folders", entry.value.mapNotNull { if (it.enable) it.absolutePath else null }.toTypedArray())
                            .build()
                    ).build()
                )
            }
        }
        FireCrasher.install(this, object : CrashListener() {

            override fun onCrash(throwable: Throwable) {
                Toast.makeText(this@App, throwable.exceptionMessage, Toast.LENGTH_LONG).show()
                // start the recovering process
                recover()
                //you need to add your crash reporting tool here
                //Ex: Crashlytics.logException(throwable);
            }
        })

    }
}

abstract class BigTimeWorker(
    private val context: Context,
    private val workerParams: WorkerParameters
) :
    CoroutineWorker(context, workerParams), StoppableTask {

    override fun needStop() = isStopped

    override suspend fun doWork(): Result {
        val stringArray =
            workerParams.inputData.getStringArray("folders") ?: return Result.failure(
                Data.Builder().putString("error", "input empty").build()
            )
        return withContext(Dispatchers.IO) {
            val results = stringArray.asList().map {
                if (!context.checkPathPermission(it)) WorkerResult.Failure(java.lang.Exception("don't have permission")) else if (isStopped) WorkerResult.Stopped
                else work(context, it)
            }
            if (results.none { it is WorkerResult.Failure || it is WorkerResult.Stopped }) Result.success()
            else Result.failure(
                Data.Builder().putString(
                    "data",
                    results.joinToString(",") {
                        when (it) {
                            is WorkerResult.Stopped -> "stop"
                            is WorkerResult.Failure -> it.exception.exceptionMessage
                            else -> ""
                        }
                    }).build()
            )
        }
    }

    abstract suspend fun work(context: Context, path: String): WorkerResult
}

class FolderWorker(context: Context, workerParams: WorkerParameters) :
    BigTimeWorker(context, workerParams) {
    override suspend fun work(context: Context, path: String): WorkerResult {
        return try {
            val fileInstance = FileInstanceFactory.getFileInstance(path, context)
            val record = context.requireDatabase.sizeDao().search(path)
            if (record != null && record.lastUpdateTime > fileInstance.directory.lastModifiedTime) return WorkerResult.SizeWorker(
                record.size
            )
            val listSafe = fileInstance.listSafe()
            val mapNullNull = listSafe.directories.map {
                if (isStopped) return WorkerResult.Stopped
                work(context, it.fullPath)
            }
            val filter =
                mapNullNull.filter { it is WorkerResult.Failure || it is WorkerResult.Stopped }
            if (filter.valid()) {
                return filter.first()
            }

            val filesSize =
                listSafe.files.map {
                    if (isStopped) return WorkerResult.Stopped
                    it.size
                }.plus(0).reduce { acc, s ->
                    if (isStopped) return WorkerResult.Stopped
                    acc + s
                }
            val size =
                filesSize + mapNullNull.map { (it as WorkerResult.SizeWorker).size }.plus(0)
                    .reduce { acc, s ->
                        if (isStopped) return WorkerResult.Stopped
                        acc + s
                    }
            context.requireDatabase.sizeDao()
                .save(FileSizeRecord(path, size, System.currentTimeMillis()))
            WorkerResult.SizeWorker(size)
        } catch (e: Exception) {
            Log.e(TAG, "work: ", e)
            WorkerResult.Failure(e)
        }

    }

    companion object {
        private const val TAG = "App"
    }

}

class MDWorker(context: Context, workerParams: WorkerParameters) :
    BigTimeWorker(context, workerParams) {

    override suspend fun work(context: Context, path: String): WorkerResult {
        return try {
            val fileInstance = FileInstanceFactory.getFileInstance(path, context)
            val listSafe = fileInstance.listSafe()
            listSafe.directories.mapNullNull {
                if (isStopped) return WorkerResult.Stopped
                work(context, it.fullPath)
            }
            listSafe.files.forEach {
                if (isStopped) return WorkerResult.Stopped
                val search = context.requireDatabase.mdDao().search(it.fullPath)
                if ((search?.lastUpdateTime ?: 0) <= it.lastModifiedTime) {
                    processAndSave(fileInstance, it, context)
                }
            }
            WorkerResult.Success
        } catch (e: Exception) {
            WorkerResult.Failure(e)
        }

    }

    private suspend fun processAndSave(fileInstance: FileInstance, it: FileItemModel, context: Context) {
        getFileMD5(
            fileInstance.toChild(it.name, true, false), closeable
        )?.let { data ->
            context.requireDatabase.mdDao()
                .save(FileMDRecord(it.fullPath, data, System.currentTimeMillis()))
        }
    }

    companion object

}

class TorrentWorker(context: Context, workerParams: WorkerParameters) :
    BigTimeWorker(context, workerParams) {

    override suspend fun work(context: Context, path: String): WorkerResult {
        return try {
            val fileInstance = FileInstanceFactory.getFileInstance(path, context)
            val listSafe = fileInstance.listSafe()
            listSafe.directories.mapNullNull {
                if (isStopped) return WorkerResult.Stopped
                work(context, it.fullPath)
            }
            listSafe.files.filterIsInstance<TorrentFileModel>().forEach {
                if (isStopped) return WorkerResult.Stopped
                val search = context.requireDatabase.torrentDao().search(it.fullPath)
                if ((search?.lastUpdateTime ?: 0) <= it.lastModifiedTime) {
                    processAndSave(fileInstance, it, context)
                }
            }
            WorkerResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "work: ", e)
            WorkerResult.Failure(e)
        }

    }

    private suspend fun processAndSave(fileInstance: FileInstance, it: TorrentFileModel, context: Context) {
        TorrentFile.getTorrentName(
            fileInstance.toChild(it.name, true, false),
            closeable
        ).takeIf { it.isNotEmpty() }?.let { torrentName ->
            context.requireDatabase.torrentDao()
                .save(
                    FileTorrentRecord(
                        it.fullPath,
                        torrentName,
                        System.currentTimeMillis()
                    )
                )
        }
    }

    companion object {
        private const val TAG = "App"
    }

}

sealed class WorkerResult {
    object Success : WorkerResult()
    class Failure(val exception: Exception) : WorkerResult()
    object Stopped : WorkerResult()
    class SizeWorker(val size: Long) : WorkerResult()
}

/**
 * ???????????????????????????????????????????????????????????? null
 */
inline fun <T, R> List<T>.mapNullNull(
    transform: (T) -> R?
): List<R>? {
    val destination = ArrayList<R>()
    var hasNull = false
    for (item in this) {
        val element = transform(item)
        if (element != null) {
            destination.add(element)
        } else if (!hasNull) {
            hasNull = true
        }
    }
    return if (hasNull) null else destination
}

const val pc_end_on = 1024

fun getFileMD5(fileInstance: FileInstance, mdWorker: StoppableTask): String? {
    val buffer = ByteArray(pc_end_on)
    return try {
        var len: Int
        val digest = MessageDigest.getInstance("MD5")
        fileInstance.bufferedInputSteam.use { stream ->
            while (stream.read(buffer).also { len = it } != -1) {
                if (mdWorker.needStop()) return null
                digest.update(buffer, 0, len)
            }
        }
        val bigInt = BigInteger(1, digest.digest())
        bigInt.toString(16)
    } catch (e: Exception) {
        null
    }
}

class WorkerStoppableTask(private val worker: ListenableWorker) : StoppableTask {
    override fun needStop() = worker.isStopped
}

val ListenableWorker.closeable get() = WorkerStoppableTask(this)

fun <T> Collection<T>?.valid(): Boolean = this != null && !isEmpty()