package com.storyteller_f.giant_explorer.control

import android.app.Application
import android.content.*
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.DragStartHelper
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.common_ktx.context
import com.storyteller_f.common_ktx.exceptionMessage
import com.storyteller_f.common_ui.*
import com.storyteller_f.common_vm_ktx.*
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.FileSystemUriSaver
import com.storyteller_f.file_system.checkPathPermission
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.local.DocumentLocalFileInstance
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.file_system.model.TorrentFileItemModel
import com.storyteller_f.file_system.requestPermissionForSpecialPath
import com.storyteller_f.file_system_ktx.fileIcon
import com.storyteller_f.file_system_ktx.isDirectory
import com.storyteller_f.filter_core.Filter
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.database.FileSizeRecordDatabase
import com.storyteller_f.giant_explorer.databinding.ActivityMainBinding
import com.storyteller_f.giant_explorer.databinding.ViewHolderFileBinding
import com.storyteller_f.giant_explorer.dialog.FileOperationDialog
import com.storyteller_f.giant_explorer.filter.*
import com.storyteller_f.giant_explorer.model.FileModel
import com.storyteller_f.giant_explorer.pc_end_on
import com.storyteller_f.giant_explorer.service.FileOperateBinder
import com.storyteller_f.giant_explorer.service.FileOperateService
import com.storyteller_f.giant_explorer.service.FileService
import com.storyteller_f.giant_explorer.view.PathMan
import com.storyteller_f.multi_core.StoppableTask
import com.storyteller_f.sort_ui.SortChain
import com.storyteller_f.sort_ui.SortDialog
import com.storyteller_f.ui_list.adapter.SimpleSourceAdapter
import com.storyteller_f.ui_list.core.*
import com.storyteller_f.ui_list.data.SimpleResponse
import com.storyteller_f.ui_list.event.findActionReceiverOrNull
import com.storyteller_f.ui_list.event.viewBinding
import com.storyteller_f.ui_list.source.SimpleSearchViewModel
import com.storyteller_f.ui_list.source.observerInScope
import com.storyteller_f.ui_list.ui.ListWithState
import com.storyteller_f.ui_list.ui.valueContains
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import com.topjohnwu.superuser.nio.FileSystemManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.ref.WeakReference
import java.util.*
import kotlin.concurrent.thread
import kotlin.coroutines.resumeWithException

class FileExplorerSession(application: Application, path: String, root: String) : AndroidViewModel(application) {
    val selected = MutableLiveData<MutableList<Pair<DataItemHolder, Int>>>()
    val fileInstance = MutableLiveData<FileInstance>()

    init {
        viewModelScope.launch {
            getFileInstanceAsync(path, application.applicationContext, root).let {
                fileInstance.value = it
            }
        }
    }
}

suspend fun getFileInstanceAsync(path: String, context: Context, root: String = FileInstanceFactory.publicFileSystemRoot) = suspendCancellableCoroutine {
    thread {
        val result = Result.success(getFileInstance(path, context, root, it.stoppable()))
        it.resumeWith(result)
    }
}

fun getFileInstance(path: String, context: Context, root: String = FileInstanceFactory.publicFileSystemRoot, stoppableTask: StoppableTask = StoppableTask.Blocking) =
    FileInstanceFactory.getFileInstance(path, context, root, stoppableTask)

class MainActivity : CommonActivity(), FileOperateService.FileOperateResultContainer {

    private val binding by viewBinding(ActivityMainBinding::inflate)
    private val filterHiddenFile by svm({}) { it, _ ->
        StateValueModel(it, FileListFragment.filterHiddenFileKey, false)
    }
    private val dialogImpl = FilterDialogManager()

    private val filters by keyPrefix({ "filter" }, svm({ dialogImpl.filterDialog }, vmProducer = buildFilterDialogState))

    private val sort by keyPrefix({ "sort" }, svm({ dialogImpl.sortDialog }, vmProducer = buildSortDialogState))

    private val uuid by vm({}) {
        GenericValueModel<String>().apply {
            data.value = UUID.randomUUID().toString()
        }
    }

    private var currentRequestingKey: String? = null
    private val requestDocumentProvider = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        processDocumentProvider(it)
    }

    private fun processDocumentProvider(uri: Uri?) {
        val key = currentRequestingKey
        Log.i(TAG, "uri: $uri $key")
        if (uri != null && key != null) {
            if (key != uri.authority) {
                Toast.makeText(this, "选择错误", Toast.LENGTH_LONG).show()
                return
            }
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            FileSystemUriSaver.getInstance().saveUri(key, this, uri)
            currentRequestingKey = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uuid
        setSupportActionBar(binding.toolbar)
        supportNavigatorBarImmersive(binding.root)
        dialogImpl.init(this, {
            filters.data.value = it
        }, {
            sort.data.value = it
        })
        filters
        sort

        //连接服务
        val fileOperateIntent = Intent(this, FileOperateService::class.java)
        startService(fileOperateIntent)
        bindService(fileOperateIntent, connection, 0)

        Shell.getShell {
            if (it.isRoot) {
                val intent = Intent(this, FileService::class.java)
                //连接服务
                RootService.bind(intent, fileConnection)
            }

        }
        binding.switchRoot.setOnClick {
            openContextMenu(it)
        }
        registerForContextMenu(binding.switchRoot)
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        val navController = navHostFragment.navController

        scope.launch {
            callbackFlow {
                binding.pathMan.setPathChangeListener(object : PathMan.PathChangeListener {
                    override fun onSkipOnPathMan(pathString: String) {
                        trySend(pathString)
                    }

                    override fun root(): String {
                        return FileInstanceFactory.publicFileSystemRoot
                    }

                })
                awaitClose {
                    binding.pathMan.setPathChangeListener(null)
                }
            }.flowWithLifecycle(lifecycle).collectLatest {
                navController.navigate(R.id.fileListFragment, FileListFragmentArgs(it, FileInstanceFactory.publicFileSystemRoot).toBundle())
            }
        }
        navController.setGraph(R.navigation.nav_main, FileListFragmentArgs(FileInstanceFactory.rootUserEmulatedPath, FileInstanceFactory.publicFileSystemRoot).toBundle())
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.filterHiddenFile)?.updateIcon(filterHiddenFile.data.value == true)
        menu.findItem(R.id.paste_file)?.let {
            it.isEnabled = ContextCompat.getSystemService(this, ClipboardManager::class.java)?.hasPrimaryClip() == true
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.filterHiddenFile -> {
                val newState = filterHiddenFile.data.value?.not() ?: true
                item.updateIcon(newState)
                filterHiddenFile.data.value = newState
            }

            R.id.newWindow -> startActivity(Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            })

            R.id.filter -> dialogImpl.showFilter()
            R.id.sort -> dialogImpl.showSort()
            R.id.open_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.open_root_access -> startActivity(Intent(this, RootAccessActivity::class.java))
            R.id.about -> startActivity(Intent(this, AboutActivity::class.java))
            R.id.plugin_manager -> startActivity(Intent(this, PluginManageActivity::class.java))

        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        menu ?: return
        val provider = Intent("android.content.action.DOCUMENTS_PROVIDER")
        val info = packageManager.queryIntentContentProvidersCompat(provider, 0)
        val savedUris = FileSystemUriSaver.getInstance().savedUris(this)
        lifecycleScope.launch {

            info.forEach {
                val authority = it.providerInfo.authority
                val root = Uri.Builder().scheme("content").authority(authority).build().toString()
                val loadLabel = it.loadLabel(packageManager).toString()
//            val icon = it.loadIcon(packageManager)
                val contains = savedUris.contains(authority) && try {
                    DocumentLocalFileInstance(this@MainActivity, "/", authority, root).exists()
                } catch (_: Exception) {
                    false
                }
                menu.add(loadLabel)
                    .setChecked(contains)
                    .setCheckable(true)
//                .setActionView(ImageView(this).apply {
//                    setImageDrawable(icon)
//                })
                    .apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            tooltipText = authority
                        }
                    }
                    .setOnMenuItemClickListener {
                        switchRoot(authority)
                        true
                    }

            }
        }
    }

    private fun switchRoot(authority: String): Boolean {
        val savedUri = FileSystemUriSaver.getInstance().savedUri(authority, this)
        if (savedUri != null) {
            try {
                val root = Uri.Builder().scheme("content").authority(authority).build().toString()
                val instance = DocumentLocalFileInstance(this, "/", authority, root)
                if (instance.exists()) {
                    findNavControl().navigate(R.id.fileListFragment, FileListFragmentArgs(instance.path, root.reversed()).toBundle())
                    return true
                }
            } catch (e: Exception) {
                Log.e(TAG, "switchRoot: ", e)
            }

        }
        currentRequestingKey = authority
        requestDocumentProvider.launch(null)
        return false
    }

    private fun findNavControl(): NavController {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        return navHostFragment.navController
    }

    private fun MenuItem.updateIcon(newState: Boolean) {
        isChecked = newState
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            iconTintList = if (newState) ColorStateList.valueOf(Color.GRAY) else ColorStateList.valueOf(Color.BLACK)
        }
    }

    var fileOperateBinder: FileOperateBinder? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Toast.makeText(this@MainActivity, "服务已连接", Toast.LENGTH_SHORT).show()
            val fileOperateBinderLocal = service as FileOperateBinder
            Log.i(TAG, "onServiceConnected: $fileOperateBinderLocal")
            fileOperateBinder = fileOperateBinderLocal
            fileOperateBinderLocal.let { binder ->
                binder.fileOperateResultContainer = WeakReference(this@MainActivity)
                binder.state.toDiffNoNull { i, i2 ->
                    i == i2
                }.observe(this@MainActivity, Observer {
                    Toast.makeText(this@MainActivity, "${it.first} ${it.second}", Toast.LENGTH_SHORT).show()
                    if (it.first == FileOperateBinder.state_null) {
                        FileOperationDialog().apply {
                            this.binder = fileOperateBinderLocal
                        }.show(supportFragmentManager, FileOperationDialog.tag)
                    }
                })
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            fileOperateBinder = null
            Toast.makeText(this@MainActivity, "服务已关闭", Toast.LENGTH_SHORT).show()
        }
    }

    private val fileConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            service?.let {
                FileSystemUriSaver.getInstance().remote = FileSystemManager.getRemote(service)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            FileSystemUriSaver.getInstance().remote = null
        }
    }

    override fun onSuccess(dest: String?, origin: String?) {
        scope.launch {
            Toast.makeText(this@MainActivity, "dest $dest origin $origin", Toast.LENGTH_SHORT).show()
        }
//        adapter.refresh()
    }

    override fun onError(string: String?) {
        scope.launch {
            Toast.makeText(this@MainActivity, "error: $string", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCancel() {
        scope.launch {
            Toast.makeText(this@MainActivity, "cancel", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unbindService(connection)
        } catch (e: Exception) {
            Toast.makeText(this, e.exceptionMessage, Toast.LENGTH_LONG).show()
        }
    }

    fun drawPath(path: String) {
        binding.pathMan.drawPath(path)
    }
}

fun LifecycleOwner.supportDirectoryContent(
    listWithState: ListWithState,
    adapter: SimpleSourceAdapter<FileItemHolder, FileViewHolder>,
    data: SimpleSearchViewModel<FileModel, FileExplorerSearch, FileItemHolder>,
    session: FileExplorerSession,
    filterHiddenFileLiveData: LiveData<Boolean>,
    filtersLiveData: LiveData<MutableList<Filter<FileSystemItemModel>>>,
    sortLivedata: LiveData<MutableList<SortChain<FileSystemItemModel>>>,
    updatePathMan: (String) -> Unit
) {
    val owner = if (this is Fragment) viewLifecycleOwner else this
    context {
        listWithState.sourceUp(adapter, owner, session.selected, flash = ListWithState.Companion::remote)
        session.fileInstance.observe(owner, Observer {
            updatePathMan(it.path)
        })
        combineDao(session.fileInstance, filterHiddenFileLiveData, filtersLiveData.distinctUntilChangedBy { filters1, filters2 ->
            filters1.same(filters2)
        }, sortLivedata.distinctUntilChangedBy { sort1, sort2 ->
            sort1.same(sort2)
        }).observe(owner, Observer {
            val fileInstance = it.d1 ?: return@Observer
            val filterHiddenFile = it.d2 ?: return@Observer
            val filters = it.d3 ?: return@Observer
            val sort = it.d4 ?: return@Observer
            val path = fileInstance.path
            //检查权限
            owner.lifecycleScope.launch {
                if (!checkPathPermission(path)) {
                    requestPermissionForSpecialPath(path)
                }
            }

            data.observerInScope(owner, FileExplorerSearch(fileInstance, filterHiddenFile, filters, sort)) { pagingData ->
                adapter.submitData(pagingData)
            }
        })

    }
}

class FileItemHolder(
    val file: FileModel,
    val selected: MutableLiveData<MutableList<Pair<DataItemHolder, Int>>>
) : DataItemHolder {
    override fun areItemsTheSame(other: DataItemHolder) =
        (other as FileItemHolder).file.fullPath == file.fullPath

    override fun areContentsTheSame(other: DataItemHolder): Boolean {
        return (other as FileItemHolder).file == file
    }

}

@BindItemHolder(FileItemHolder::class)
class FileViewHolder(private val binding: ViewHolderFileBinding) :
    AdapterViewHolder<FileItemHolder>(binding) {
    override fun bindData(itemHolder: FileItemHolder) {
        binding.fileName.text = itemHolder.file.name
        binding.fileIcon.fileIcon(itemHolder.file.item)
        val item = itemHolder.file.item
        binding.root.isSelected = itemHolder.selected.value?.valueContains(
            Pair(itemHolder, 0)
        ) == true
        binding.root.setBackgroundResource(
            if (itemHolder.file.item is FileItemModel) R.drawable.background_file else R.drawable.background_folder
        )
        binding.fileSize.setVisible(item.size != -1L) {
            it.text = item.formattedSize
        }
        binding.fileMD.setVisible(item, { it: FileItemModel -> it.md.valid() }) { it, i ->
            it.text = i.md
        }

        binding.torrentName.setVisible(item, { it: TorrentFileItemModel -> it.torrentName.valid() }) { it, i ->
            it.text = i.torrentName
        }

        binding.modifiedTime.setVisible(item.lastModifiedTime > 0 && item.formattedLastModifiedTime.valid()) {
            it.text = item.formattedLastModifiedTime
        }

        binding.detail.text = item.permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            dragSupport(itemHolder)
        }
        binding.symLink.isVisible = itemHolder.file.isSymLink

    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun dragSupport(itemHolder: FileItemHolder) {
        val listener = { view: View, _: DragStartHelper ->
            val clipData = ClipData.newPlainText(FileListFragment.clipDataKey, itemHolder.file.fullPath)
            val flags = View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
            view.startDragAndDrop(clipData, View.DragShadowBuilder(view), null, flags)
        }
        DragStartHelper(binding.root, listener).apply {
            attach()
        }
        if (itemHolder.file.item.isDirectory)
            binding.root.setOnDragListener { v, event ->
                when (event.action) {
                    DragEvent.ACTION_DRAG_STARTED -> {
                        val clipDescription = event.clipDescription
                        clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                                && clipDescription.label == FileListFragment.clipDataKey
                    }

                    DragEvent.ACTION_DROP -> {
                        v.findActionReceiverOrNull<FileListFragment>()?.handleClipData(event.clipData, itemHolder.file.fullPath)
                        true
                    }

                    else -> true
                }
            }
        else binding.root.setOnDragListener(null)
    }
}

fun String?.valid() = this?.trim()?.isNotEmpty() == true


fun format1024(args: Long): String {
    if (args < 0) {
        return "Error"
    }
    val flags = arrayOf("B.", "KB", "MB", "GB", "TB")
    var flag = 0
    var size = args.toDouble()
    while (size >= pc_end_on) {
        if (Thread.currentThread().isInterrupted) {
            return "stopped"
        }
        size /= pc_end_on
        flag += 1
    }
    assert(flag < flags.size) {
        "$flag $size $args"
    }
    return String.format(Locale.CHINA, "%.2f %s", size, flags[flag])
}

class FileExplorerSearch(val path: FileInstance, val filterHiddenFile: Boolean, val filters: List<Filter<FileSystemItemModel>>, val sort: List<SortChain<FileSystemItemModel>>)

fun fileServiceBuilder(
    database: FileSizeRecordDatabase
): suspend (searchQuery: FileExplorerSearch, start: Int, count: Int) -> SimpleResponse<FileModel> {
    return { searchQuery: FileExplorerSearch, start: Int, count: Int ->
        val listSafe = suspendCancellableCoroutine { continuation ->
            thread {
                try {
                    searchQuery.path.also {
                        if (it is DocumentLocalFileInstance) {
                            if (!it.exists()) {
                                it.initDocumentFile()
                            }
                        }
                    }.listSafe().run {
                        continuation.resumeWith(Result.success(this))
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }

            }
        }

        val predicate: (FileSystemItemModel) -> Boolean = {
            (!it.isHidden || !searchQuery.filterHiddenFile) && (searchQuery.filters.isEmpty() || searchQuery.filters.any { f ->
                f.filter(it)
            })
        }
        val directories = listSafe.directories.toList()
        val files = listSafe.files.toList()
        if (searchQuery.sort.isNotEmpty()) {
            SortDialog.sortInternal(directories, searchQuery.sort)
            SortDialog.sortInternal(files, searchQuery.sort)
        }
        val listFiles = if (searchQuery.filterHiddenFile || searchQuery.filters.isNotEmpty()) {
            directories.filter(predicate).plus(files.filter(predicate))
        } else directories.plus(files)
        val total = listFiles.size
        val index = start - 1
        val startPosition = index * count
        if (startPosition > total) SimpleResponse(0)
        else {
            val items = listFiles
                .subList(startPosition, (count + startPosition).coerceAtMost(total))
                .map { model ->
                    fileModelBuilder(model, database)
                }
            SimpleResponse(
                total = total,
                items = items,
                if (total > count * start) start + 1 else null
            )
        }

    }

}

private suspend fun fileModelBuilder(
    model: FileSystemItemModel,
    database: FileSizeRecordDatabase
): FileModel {
    val length = if (model is FileItemModel) {
        database.mdDao().search(model.fullPath)?.let {
            if (it.lastUpdateTime > model.lastModifiedTime) model.md = it.data
        }
        if (model is TorrentFileItemModel)
            database.torrentDao().search(model.fullPath)?.let {
                if (it.lastUpdateTime > model.lastModifiedTime) model.torrentName = it.torrent
            }
        model.size
    } else {
        //从数据库中查找
        val directory = database.sizeDao().search(model.fullPath)
        if (directory != null && directory.lastUpdateTime > model.lastModifiedTime) {
            directory.size
        } else -1
    }
    model.formattedSize = format1024(length)
    model.size = length
    return FileModel(model.name, model.fullPath, length, model.isHidden, model, model.isSymLink)
}

fun <T> List<T>.same(list: List<T>): Boolean {
    if (size != list.size) return false
    forEachIndexed { index, t ->
        if (list[index] != t) {
            return false
        }
    }
    return true
}

/**
 * @param f 返回是否相等
 */
fun <X> LiveData<X>.distinctUntilChangedBy(f: (X, X) -> Boolean): LiveData<X?> {
    val outputLiveData: MediatorLiveData<X?> = MediatorLiveData<X?>()
    outputLiveData.addSource(this, object : Observer<X?> {
        var mFirstTime = true
        var previous: X? = null
        override fun onChanged(value: X?) {
            val previousValue = previous
            if (mFirstTime || previousValue == null && value != null || previousValue != null && (previousValue != value || !f(previousValue, value))) {
                mFirstTime = false
                outputLiveData.value = value
                previous = value
            }
        }
    })
    return outputLiveData
}

@Suppress("DEPRECATION")
fun PackageManager.queryIntentActivitiesCompat(searchDocumentProvider: Intent, flags: Long): MutableList<ResolveInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentActivities(searchDocumentProvider, PackageManager.ResolveInfoFlags.of(flags))
    } else {
        queryIntentActivities(searchDocumentProvider, flags.toInt())
    }
}

@Suppress("DEPRECATION")
fun PackageManager.queryIntentContentProvidersCompat(searchDocumentProvider: Intent, flags: Long): MutableList<ResolveInfo> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        queryIntentContentProviders(searchDocumentProvider, PackageManager.ResolveInfoFlags.of(flags))
    } else {
        queryIntentContentProviders(searchDocumentProvider, flags.toInt())
    }
}