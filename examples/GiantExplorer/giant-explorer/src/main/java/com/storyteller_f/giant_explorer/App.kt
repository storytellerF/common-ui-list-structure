package com.storyteller_f.giant_explorer

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.material.color.DynamicColors
import com.storyteller_f.common_ktx.exceptionMessage
import com.storyteller_f.config_core.EditorKey
import com.storyteller_f.config_core.editor
import com.storyteller_f.file_system.checkPathPermission
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.TorrentFileItemModel
import com.storyteller_f.file_system_ktx.getFileInstance
import com.storyteller_f.filter_core.config.FilterConfig
import com.storyteller_f.filter_core.config.FilterConfigItem
import com.storyteller_f.filter_ui.FilterDialog
import com.storyteller_f.giant_explorer.control.plugin.PluginManager
import com.storyteller_f.giant_explorer.control.ui_list.HolderBuilder
import com.storyteller_f.giant_explorer.database.FileMDRecord
import com.storyteller_f.giant_explorer.database.FileSizeRecord
import com.storyteller_f.giant_explorer.database.FileTorrentRecord
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.dialog.FilterDialogFragment
import com.storyteller_f.giant_explorer.dialog.SortDialogFragment
import com.storyteller_f.giant_explorer.dialog.activeFilters
import com.storyteller_f.giant_explorer.dialog.activeSortChains
import com.storyteller_f.giant_explorer.dialog.buildFilters
import com.storyteller_f.giant_explorer.dialog.buildSorts
import com.storyteller_f.giant_explorer.utils.TorrentFile
import com.storyteller_f.multi_core.StoppableTask
import com.storyteller_f.sort_core.config.SortConfig
import com.storyteller_f.sort_core.config.SortConfigItem
import com.storyteller_f.sort_ui.SortDialog
import com.storyteller_f.ui_list.core.holders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.spongycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.security.Security

val pluginManagerRegister = PluginManager()

val defaultFactory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return super.create(modelClass, extras)
    }
}

object WorkCategory {
    const val messageDigest = "message-digest"
    const val folderSize = "folder-size"
    const val torrentName = "torrent-name"
}

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        setupBouncyCastle()
        holders(HolderBuilder::add)
        MainScope().launch {
            requireDatabase.bigTimeDao().fetchSuspend().groupBy {
                it.category
            }.forEach { entry ->
                WorkManager.getInstance(this@App).enqueueUniqueWork(
                    entry.key,
                    ExistingWorkPolicy.KEEP,
                    when (entry.key) {
                        WorkCategory.messageDigest -> OneTimeWorkRequestBuilder<MDWorker>()
                        WorkCategory.folderSize -> OneTimeWorkRequestBuilder<FolderWorker>()
                        else -> OneTimeWorkRequestBuilder<TorrentWorker>()
                    }.setInputData(
                        Data.Builder().putStringArray("folders", entry.value.mapNotNull {
                            when {
                                !it.enable -> null
                                else -> it.uri.toString()
                            }
                        }.toTypedArray())
                            .build()
                    ).build()
                )
            }
            refreshPlugin(this@App)
        }
        activeFilters.value = EditorKey.createEditorKey(filesDir.absolutePath, FilterDialogFragment.suffix).editor(FilterConfig.emptyFilterListener, FilterDialog.configAdapterFactory, FilterDialogFragment.factory).lastConfig?.run {
            configItems.filterIsInstance<FilterConfigItem>().buildFilters()
        }
        activeSortChains.value = EditorKey.createEditorKey(filesDir.absolutePath, SortDialogFragment.suffix).editor(SortConfig.emptySortListener, SortDialog.configAdapterFactory, SortDialogFragment.adapterFactory).lastConfig?.run {
            configItems.filterIsInstance<SortConfigItem>().buildSorts()
        }
    }

    private fun setupBouncyCastle() {
        Security.removeProvider("BC")
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }

}

fun refreshPlugin(context: Context) {
    File(context.filesDir, "plugins").listFiles { it ->
        it.extension == "apk" || it.extension == "zip"
    }?.forEach {
        pluginManagerRegister.foundPlugin(it)
    }
}

abstract class BigTimeWorker(
    private val context: Context,
    private val workerParams: WorkerParameters
) :
    CoroutineWorker(context, workerParams), StoppableTask {

    override fun needStop() = isStopped

    override suspend fun doWork(): Result {
        val uriStringArray =
            workerParams.inputData.getStringArray("folders") ?: return Result.failure(
                Data.Builder().putString("error", "input empty").build()
            )
        return withContext(Dispatchers.IO) {
            val results = uriStringArray.asList().map { uriString ->
                when {
                    !context.checkPathPermission(uriString.toUri()) -> WorkerResult.Failure(java.lang.Exception("don't have permission"))
                    isStopped -> WorkerResult.Stopped
                    else -> doWork(context, uriString)
                }
            }
            if (results.none { it is WorkerResult.Failure || it is WorkerResult.Stopped }) Result.success()
            else Result.failure(
                Data.Builder().putString(
                    "error",
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

    abstract suspend fun doWork(context: Context, uriString: String): WorkerResult
}

class FolderWorker(context: Context, workerParams: WorkerParameters) :
    BigTimeWorker(context, workerParams) {
    override suspend fun doWork(context: Context, uriString: String): WorkerResult {
        return try {
            val uri = uriString.toUri()
            val fileInstance = getFileInstance(context, uri)
            val record = context.requireDatabase.sizeDao().search(uri)
            if (record != null && record.lastUpdateTime > fileInstance.getDirectory().lastModifiedTime) return WorkerResult.SizeWorker(
                record.size
            )
            val listSafe = fileInstance.list()
            val mapNullNull = listSafe.directories.map {
                if (isStopped) return WorkerResult.Stopped
                doWork(context, it.fullPath)
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
                .save(FileSizeRecord(uri, size, System.currentTimeMillis()))
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

    override suspend fun doWork(context: Context, uriString: String): WorkerResult {
        return try {
            val uri = uriString.toUri()
            val fileInstance = getFileInstance(context, uri)
            val listSafe = fileInstance.list()
            listSafe.directories.mapNullNull {
                if (isStopped) return WorkerResult.Stopped
                doWork(context, it.fullPath)
            }
            listSafe.files.forEach {
                if (isStopped) return WorkerResult.Stopped
                val child = uri.buildUpon().path(it.fullPath).build()
                val search = context.requireDatabase.mdDao().search(child)
                if ((search?.lastUpdateTime ?: 0) <= it.lastModifiedTime) {
                    processAndSave(fileInstance, it, context, child)
                }
            }
            WorkerResult.Success
        } catch (e: Exception) {
            WorkerResult.Failure(e)
        }

    }

    private suspend fun processAndSave(
        fileInstance: FileInstance,
        it: FileItemModel,
        context: Context,
        child: Uri
    ) {
        getFileMD5(
            fileInstance.toChild(it.name, FileCreatePolicy.NotCreate)!!, closeable
        )?.let { data ->
            context.requireDatabase.mdDao()
                .save(FileMDRecord(child, data, System.currentTimeMillis()))
        }
    }

    companion object

}

class TorrentWorker(context: Context, workerParams: WorkerParameters) :
    BigTimeWorker(context, workerParams) {

    override suspend fun doWork(context: Context, uriString: String): WorkerResult {
        return try {
            val uri = uriString.toUri()
            val fileInstance = getFileInstance(context, uri)
            val listSafe = fileInstance.list()
            listSafe.directories.mapNullNull {
                if (isStopped) return WorkerResult.Stopped
                doWork(context, it.fullPath)
            }
            listSafe.files.filterIsInstance<TorrentFileItemModel>().forEach {
                if (isStopped) return WorkerResult.Stopped
                val child = uri.buildUpon().path(it.fullPath).build()
                val search = context.requireDatabase.torrentDao().search(child)
                if ((search?.lastUpdateTime ?: 0) <= it.lastModifiedTime) {
                    processAndSave(fileInstance, it, context, child)
                }
            }
            WorkerResult.Success
        } catch (e: Exception) {
            Log.e(TAG, "work: ", e)
            WorkerResult.Failure(e)
        }

    }

    private suspend fun processAndSave(
        fileInstance: FileInstance,
        it: TorrentFileItemModel,
        context: Context,
        child: Uri
    ) {
        TorrentFile.getTorrentName(
            fileInstance.toChild(it.name, FileCreatePolicy.NotCreate)!!,
            closeable
        ).takeIf { it.isNotEmpty() }?.let { torrentName ->
            context.requireDatabase.torrentDao()
                .save(
                    FileTorrentRecord(
                        child,
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
 * 不影响后续文件夹执行，但是最终返回值会是 null
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

suspend fun getFileMD5(fileInstance: FileInstance, mdWorker: StoppableTask): String? {
    val buffer = ByteArray(pc_end_on)
    return try {
        var len: Int
        val digest = MessageDigest.getInstance("MD5")
        fileInstance.getFileInputStream().buffered().use { stream ->
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

fun <T> Collection<T>?.valid(): Boolean = !isNullOrEmpty()