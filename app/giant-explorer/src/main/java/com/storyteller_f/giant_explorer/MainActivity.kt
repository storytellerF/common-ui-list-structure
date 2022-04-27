package com.storyteller_f.giant_explorer

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.annotation_defination.BindLongClickEvent
import com.storyteller_f.common_ktx.context
import com.storyteller_f.common_ktx.contextSuspend
import com.storyteller_f.common_ktx.mm
import com.storyteller_f.common_ui.SimpleActivity
import com.storyteller_f.common_ui.scope
import com.storyteller_f.common_ui.setVisible
import com.storyteller_f.common_ui.supportNavigatorBarImmersive
import com.storyteller_f.common_vm_ktx.combine
import com.storyteller_f.common_vm_ktx.toDiff
import com.storyteller_f.common_vm_ktx.vm
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
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.ActivityMainBinding
import com.storyteller_f.giant_explorer.databinding.ViewHolderFileBinding
import com.storyteller_f.giant_explorer.dialog.NewNameDialog
import com.storyteller_f.giant_explorer.dialog.OpenFileDialog
import com.storyteller_f.giant_explorer.dialog.RequestPathDialog
import com.storyteller_f.giant_explorer.model.FileModel
import com.storyteller_f.giant_explorer.service.FileOperateBinder
import com.storyteller_f.giant_explorer.service.FileOperateService
import com.storyteller_f.giant_explorer.view.EditablePathMan
import com.storyteller_f.ui_list.core.*
import com.storyteller_f.ui_list.data.SimpleResponse
import com.storyteller_f.ui_list.event.viewBinding
import com.storyteller_f.ui_list.ui.ListWithState
import com.storyteller_f.ui_list.ui.valueContains
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.min

class FileExplorerSession : ViewModel() {
    val filterHiddenFile = MutableLiveData(false)
    val selected = MutableLiveData<MutableList<Pair<DataItemHolder, Int>>>()
    val fileInstance = MutableLiveData<FileInstance>()

    fun init(owner: LifecycleOwner) {
        owner.context {
            if (fileInstance.value == null)
                owner.scope.launch {
                    suspendCancellableCoroutine<FileInstance> {
                        thread {
                            val result = Result.success(FileInstanceFactory.getFileInstance("/storage/emulated/0", context = this@context))
                            it.resumeWith(result)
                        }
                    }.let {
                        fileInstance.value = it
                    }
                }
        }
    }

}

class MainActivity : SimpleActivity(), FileOperateService.FileOperateResult {
    private val data by search(
        SearchProducer(::service) { it, _ ->
            FileItemHolder(it, session.selected)
        }
    )
    private val adapter = SimpleSourceAdapter<FileItemHolder, FileViewHolder>()
    private val binding by viewBinding(ActivityMainBinding::inflate)
    private val session by vm {
        FileExplorerSession()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbar)
        supportNavigatorBarImmersive(binding.root)
        supportDirectoryContent(binding.content, binding.pathMan, adapter, data, session)
        session.selected.toDiff().observe(this, Observer {
        })
        context {
            //连接服务
            try {
                bindService(Intent(this, FileOperateService::class.java), connection, BIND_AUTO_CREATE)
            } catch (e: Exception) {
                bindService(Intent(this, FileOperateService::class.java), connection, 0)
            }
        }
        session.init(this)
        onBackPressedDispatcher.addCallback(this) {
            val value = session.fileInstance.value
            if (value != null) {
                if (value.path == "/" || value.path == FileInstanceFactory.rootUserEmulatedPath) {
                    isEnabled = false
                    onBackPressed()
                } else {
                    session.fileInstance.value = FileInstanceFactory.toParent(value, this@MainActivity)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        MenuInflater(this).inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.filterHiddenFile)?.let {
            it.isChecked = session.filterHiddenFile.value == true
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_file -> {
                fragment<NewNameDialog.NewNameResult>("add-file", NewNameDialog::class.java) { bundle ->
                    session.fileInstance.value?.toChild(bundle.name, true, true)
                }
            }
            R.id.filterHiddenFile -> {
                session.filterHiddenFile.value = session.filterHiddenFile.value?.not() ?: true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    @BindClickEvent(FileItemHolder::class)
    fun toChild(itemHolder: FileItemHolder) {
        if (itemHolder.file.item.isDirectory) {
            val old = session.fileInstance.value ?: return
            session.fileInstance.value = FileInstanceFactory.toChild(
                old,
                itemHolder.file.name,
                false,
                this,
                false
            )
        } else {
            fragment<OpenFileDialog.OpenFileResult>(OpenFileDialog.key, OpenFileDialog::class.java, Bundle().apply {
                putString("path", itemHolder.file.fullPath)
            }) {
                Intent("android.intent.action.VIEW").apply {
                    addCategory("android.intent.category.DEFAULT")
                    val file = File(itemHolder.file.fullPath)
                    val uriForFile = FileProvider.getUriForFile(this@MainActivity, "$packageName.file-provider", file)
                    setDataAndType(uriForFile, it.mimeType)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }.let {
                    startActivity(Intent.createChooser(it, "open by"))
                }
            }
        }
    }

    @BindLongClickEvent(FileItemHolder::class)
    fun test(itemHolder: FileItemHolder) {
        RequestPathDialog().show(supportFragmentManager, "request-path")
        fragment<RequestPathDialog.RequestPathResult>("request-path", RequestPathDialog::class.java) { result ->
            result.path.mm {
                FileInstanceFactory.getFileInstance(it, this)
            }.mm {
                session.selected.value?.map { pair -> (pair.first as FileItemHolder).file.item } ?: listOf(itemHolder.file.item)
            }.let {
                println(it)
            }
        }
    }

    private var fileOperateBinder: FileOperateBinder? = null
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Toast.makeText(this@MainActivity, "服务已连接", Toast.LENGTH_SHORT).show()
            fileOperateBinder = service as FileOperateBinder
            fileOperateBinder?.setFileOperateResult(this@MainActivity)
            fileOperateBinder?.state?.observe(this@MainActivity) {
                Toast.makeText(this@MainActivity, it.toString(), Toast.LENGTH_SHORT).show()
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Toast.makeText(this@MainActivity, "服务已关闭", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSuccess(dest: String?, origin: String?) {
        TODO("Not yet implemented")
    }

    override fun onError(string: String?) {
        TODO("Not yet implemented")
    }

    override fun onCancel() {
        TODO("Not yet implemented")
    }
}

fun LifecycleOwner.supportDirectoryContent(
    listWithState: ListWithState,
    pathMan: EditablePathMan,
    adapter: SimpleSourceAdapter<FileItemHolder, FileViewHolder>,
    data: SimpleSearchViewModel<FileModel, FileExplorerSearch, FileItemHolder>,
    session: FileExplorerSession
) {
    val owner = if (this is Fragment) viewLifecycleOwner else this
    context {
        listWithState.sourceUp(adapter, lifecycleScope, session.selected, flash = ListWithState.Companion::remote)
        combine("file" to session.fileInstance, "filter" to session.filterHiddenFile).observe(owner) {
            val filter = it["filter"] as Boolean
            val file = it["file"] as FileInstance? ?: return@observe
            val path = file.path
            //检查权限
            lifecycleScope.launch {
                if (!checkPathPermission(path)) {
                    requestPermissionForSpecialPath(path)
                }
            }

            data.observerInScope(lifecycleScope, FileExplorerSearch(file, filter)) { pagingData ->
                listWithState.recyclerView.smoothScrollToPosition(0)
                adapter.submitData(pagingData)
            }
            pathMan.drawPath(path)
        }
        callbackFlow {
            pathMan.setPathChangeListener {
                trySend(it)
            }
            awaitClose {
                pathMan.setPathChangeListener(null)
            }
        }.onEach {
            session.fileInstance.value = FileInstanceFactory.getFileInstance(it, this)
        }.launchIn(owner.lifecycleScope)
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
        if (itemHolder.selected.value?.valueContains(
                Pair(itemHolder, 0)
            ) == true
        ) binding.root.setBackgroundColor(getColor(com.storyteller_f.ui_list.R.color.greyAlpha))
        else
            binding.root.setBackgroundResource(if (itemHolder.file.item is FileItemModel) com.storyteller_f.file_system.R.drawable.file_background else com.storyteller_f.file_system.R.drawable.folder_background)

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
    return String.format(Locale.CHINA, "%.2f %s", size, flags[flag])
}

class FileExplorerSearch(val path: FileInstance, val filterHiddenFile: Boolean)

suspend fun LifecycleOwner.service(
    searchQuery: FileExplorerSearch,
    start: Int,
    count: Int
) = contextSuspend {
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
                    requireDatabase().mdDao().search(model.fullPath)?.let {
                        if (it.lastUpdateTime > model.lastModifiedTime) model.md = it.data
                    }
                    if (model is TorrentFileModel)
                        requireDatabase().torrentDao().search(model.fullPath)?.let {
                            if (it.lastUpdateTime > model.lastModifiedTime) model.torrentName = it.torrent
                        }
                    model.size
                } else {
                    //从数据库中查找
                    val directory = requireDatabase().sizeDao().search(model.fullPath)
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