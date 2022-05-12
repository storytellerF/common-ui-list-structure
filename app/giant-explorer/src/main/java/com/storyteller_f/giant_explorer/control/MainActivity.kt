package com.storyteller_f.giant_explorer.control

import android.content.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.DragStartHelper
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.common_ktx.context
import com.storyteller_f.common_ktx.exceptionMessage
import com.storyteller_f.common_ui.SimpleActivity
import com.storyteller_f.common_ui.scope
import com.storyteller_f.common_ui.setVisible
import com.storyteller_f.common_ui.supportNavigatorBarImmersive
import com.storyteller_f.common_vm_ktx.*
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.checkPathPermission
import com.storyteller_f.file_system.fileIcon
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.file_system.model.FilesAndDirectories
import com.storyteller_f.file_system.model.TorrentFileModel
import com.storyteller_f.file_system.requestPermissionForSpecialPath
import com.storyteller_f.file_system_ktx.isDirectory
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.database.FileSizeRecordDatabase
import com.storyteller_f.giant_explorer.databinding.ActivityMainBinding
import com.storyteller_f.giant_explorer.databinding.ViewHolderFileBinding
import com.storyteller_f.giant_explorer.dialog.FileOperationDialog
import com.storyteller_f.giant_explorer.model.FileModel
import com.storyteller_f.giant_explorer.service.FileOperateBinder
import com.storyteller_f.giant_explorer.service.FileOperateService
import com.storyteller_f.ui_list.core.*
import com.storyteller_f.ui_list.data.SimpleResponse
import com.storyteller_f.ui_list.event.findActionReceiverOrNull
import com.storyteller_f.ui_list.event.viewBinding
import com.storyteller_f.ui_list.ui.ListWithState
import com.storyteller_f.ui_list.ui.valueContains
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.min

class FileExplorerSession : ViewModel() {
    val selected = MutableLiveData<MutableList<Pair<DataItemHolder, Int>>>()
    val fileInstance = MutableLiveData<FileInstance>()

    fun init(owner: LifecycleOwner, path: String?) {
        owner.context {
            if (fileInstance.value == null)
                owner.scope.launch {
                    suspendCancellableCoroutine<FileInstance> {
                        thread {
                            val result = Result.success(FileInstanceFactory.getFileInstance(path ?: FileInstanceFactory.rootUserEmulatedPath, context = this@context))
                            it.resumeWith(result)
                        }
                    }.let {
                        fileInstance.value = it
                    }
                }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }

}

class MainActivity : SimpleActivity(), FileOperateService.FileOperateResultContainer {

    private val binding by viewBinding(ActivityMainBinding::inflate)
    private val filterHiddenFile by svm {
        HasStateValueModel(it, FileListFragment.filterHiddenFileKey, false)
    }
    private val uuid by vm {
        GenericValueModel<String>().apply {
            data.value = UUID.randomUUID().toString()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uuid
        setSupportActionBar(binding.toolbar)
        supportNavigatorBarImmersive(binding.root)
        context {
            //连接服务
            try {
                bindService(Intent(this, FileOperateService::class.java), connection, BIND_AUTO_CREATE)
            } catch (e: Exception) {
                bindService(Intent(this, FileOperateService::class.java), connection, 0)
            }
        }
        scope.launch {
            callbackFlow {
                binding.pathMan.setPathChangeListener {
                    trySend(it)
                }
                awaitClose {
                    binding.pathMan.setPathChangeListener(null)
                }
            }.flowWithLifecycle(lifecycle).collectLatest {
                findNavController(R.id.nav_host_fragment_main).navigate(R.id.fileListFragment, Bundle().apply {
                    putString("path", it)
                })
            }
        }
        findNavController(R.id.nav_host_fragment_main).setGraph(R.navigation.nav_main, FileListFragmentArgs(FileInstanceFactory.rootUserEmulatedPath).toBundle())
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        MenuInflater(this).inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.filterHiddenFile)?.let {
            it.isChecked = filterHiddenFile.data.value == true
        }
        menu.findItem(R.id.paste_file)?.let {
            it.isEnabled = ContextCompat.getSystemService(this, ClipboardManager::class.java)?.hasPrimaryClip() == true
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.filterHiddenFile -> {
                filterHiddenFile.data.value = filterHiddenFile.data.value?.not() ?: true
            }
            R.id.newWindow -> {
                startActivity(Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                })
            }

        }
        return super.onOptionsItemSelected(item)
    }

    var fileOperateBinder: FileOperateBinder? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Toast.makeText(this@MainActivity, "服务已连接", Toast.LENGTH_SHORT).show()
            val fileOperateBinderLocal = service as FileOperateBinder
            Log.i(TAG, "onServiceConnected: $fileOperateBinderLocal")
            fileOperateBinder = fileOperateBinderLocal
            fileOperateBinderLocal.let { binder ->
                binder.fileOperateResultContainer = this@MainActivity
                binder.state.toDiffNoNull { i, i2 ->
                    i == i2 && i != FileOperateBinder.state_null
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
            Toast.makeText(this@MainActivity, "服务已关闭", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSuccess(dest: String?, origin: String?) {
        scope.launch {
            context {
                Toast.makeText(this, "dest $dest origin $origin", Toast.LENGTH_SHORT).show()
            }
        }
//        adapter.refresh()
    }

    override fun onError(string: String?) {
        scope.launch {
            context {
                Toast.makeText(this, "error: $string", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCancel() {
        scope.launch {
            context {
                Toast.makeText(this@MainActivity, "cancel", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fileOperateBinder?.fileOperateResultContainer = null
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
    filterHiddenFile: LiveData<Boolean>,
    updatePathMan: (String) -> Unit
) {
    val owner = if (this is Fragment) viewLifecycleOwner else this
    context {
        listWithState.sourceUp(adapter, owner, session.selected, flash = ListWithState.Companion::remote)
        session.fileInstance.observe(owner, Observer {
            updatePathMan(it.path)
        })
        combine("file" to session.fileInstance, "filter" to filterHiddenFile).observe(owner, Observer {
            val filter = it["filter"] as? Boolean ?: return@Observer
            val file = it["file"] as FileInstance? ?: return@Observer
            val path = file.path
            //检查权限
            owner.lifecycleScope.launch {
                if (!checkPathPermission(path)) {
                    requestPermissionForSpecialPath(path)
                }
            }

            data.observerInScope(owner, FileExplorerSearch(file, filter)) { pagingData ->
                adapter.submitData(pagingData)
            }
        })

    }
}

class FileItemHolder(
    val file: FileModel,
    val selected: MutableLiveData<MutableList<Pair<DataItemHolder, Int>>>
) : DataItemHolder() {
    override fun areItemsTheSame(other: DataItemHolder) =
        (other as FileItemHolder).file.fullPath == file.fullPath

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
        binding.fileMD.setVisible(item is FileItemModel && item.md.valid()) {
            it.text = (item as? FileItemModel)?.md
        }

        binding.torrentName.setVisible(item is TorrentFileModel && item.torrentName.valid()) {
            it.text = (item as? TorrentFileModel)?.torrentName
        }

        binding.modifiedTime.setVisible(item.lastModifiedTime > 0 && item.formattedLastModifiedTime.valid()) {
            it.text = item.formattedLastModifiedTime
        }

        binding.detail.text = item.detail
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val listener = OnDragStartListener@{ view: View, _: DragStartHelper ->
                val clipData = ClipData.newPlainText("file explorer", itemHolder.file.fullPath)
                val flags = View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
                return@OnDragStartListener view.startDragAndDrop(clipData, View.DragShadowBuilder(view), itemHolder.file, flags)
            }
            DragStartHelper(binding.root, listener).apply {
                attach()
            }
            if (itemHolder.file.item.isDirectory)
                binding.root.setOnDragListener { v, event ->
                    (event.localState as? FileModel)?.let {
                        if (itemHolder.file.fullPath.contains(it.item.fullPath)) return@setOnDragListener false
                    }
                    when (event.action) {
                        DragEvent.ACTION_DROP -> {
                            binding.root.findActionReceiverOrNull<FileListFragment>()?.handleClipData(event.clipData)
                        }
                    }
                    return@setOnDragListener true
                }
            else binding.root.setOnDragListener(null)
        }

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
    while (size >= 1024) {
        if (Thread.currentThread().isInterrupted) {
            return "stopped"
        }
        size /= 1024.0
        flag += 1
    }
    assert(flag < flags.size) {
        "$flag $size $args"
    }
    return String.format(Locale.CHINA, "%.2f %s", size, flags[flag])
}

class FileExplorerSearch(val path: FileInstance, val filterHiddenFile: Boolean)

fun service(
    database: FileSizeRecordDatabase
): suspend (searchQuery: FileExplorerSearch, start: Int, count: Int) -> SimpleResponse<FileModel> {
    return { searchQuery: FileExplorerSearch, start: Int, count: Int ->
        val listSafe = suspendCancellableCoroutine<FilesAndDirectories> {
            thread {
                searchQuery.path.listSafe().run {
                    it.resumeWith(Result.success(this))
                }
            }
        }

        val predicate: (FileSystemItemModel) -> Boolean = {
            !it.isHidden || !searchQuery.filterHiddenFile
        }
        val listFiles = if (searchQuery.filterHiddenFile) {
            listSafe.directories.filter(predicate).plus(listSafe.files.filter(predicate))
        } else listSafe.directories.plus(listSafe.files)
        val total = listFiles.size
        if ((start - 1) * count > total) SimpleResponse(0)
        else {
            val items = listFiles
                .subList((start - 1) * count, start + min(count, total - start))
                .map { model ->
                    val length = if (model is FileItemModel) {
                        database.mdDao().search(model.fullPath)?.let {
                            if (it.lastUpdateTime > model.lastModifiedTime) model.md = it.data
                        }
                        if (model is TorrentFileModel)
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
                    FileModel(model.name, model.fullPath, length, model.isHidden, model)
                }
            SimpleResponse(
                total = total,
                items = items,
                if (total > count * start) start + 1 else null
            )
        }

    }

}