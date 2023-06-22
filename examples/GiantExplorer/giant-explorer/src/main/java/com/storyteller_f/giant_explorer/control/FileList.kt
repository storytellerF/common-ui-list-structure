package com.storyteller_f.giant_explorer.control

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.net.Uri
import android.os.Build
import android.view.DragEvent
import android.view.View
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ComponentActivity
import androidx.core.view.DragStartHelper
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.savedstate.SavedStateRegistryOwner
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.common_ktx.context
import com.storyteller_f.common_ktx.same
import com.storyteller_f.common_ui.cycle
import com.storyteller_f.common_ui.owner
import com.storyteller_f.common_ui.setVisible
import com.storyteller_f.common_vm_ktx.StateValueModel
import com.storyteller_f.common_vm_ktx.VMScope
import com.storyteller_f.common_vm_ktx.combineDao
import com.storyteller_f.common_vm_ktx.distinctUntilChangedBy
import com.storyteller_f.common_vm_ktx.genericValueModel
import com.storyteller_f.common_vm_ktx.keyPrefix
import com.storyteller_f.common_vm_ktx.svm
import com.storyteller_f.common_vm_ktx.vm
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.checkPathPermission
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.file_system.model.TorrentFileItemModel
import com.storyteller_f.file_system.requestPermissionForSpecialPath
import com.storyteller_f.file_system_ktx.fileIcon
import com.storyteller_f.file_system_ktx.isDirectory
import com.storyteller_f.file_system_remote.FtpFileInstance
import com.storyteller_f.file_system_remote.FtpsFileInstance
import com.storyteller_f.file_system_remote.RemoteSpec
import com.storyteller_f.file_system_remote.SFtpFileInstance
import com.storyteller_f.file_system_remote.ShareSpec
import com.storyteller_f.file_system_remote.SmbFileInstance
import com.storyteller_f.file_system_remote.WebDavFileInstance
import com.storyteller_f.file_system_root.RootAccessFileInstance
import com.storyteller_f.filter_core.Filter
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.control.plugin.stoppable
import com.storyteller_f.giant_explorer.database.LocalDatabase
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.ViewHolderFileBinding
import com.storyteller_f.giant_explorer.databinding.ViewHolderFileGridBinding
import com.storyteller_f.giant_explorer.model.FileModel
import com.storyteller_f.giant_explorer.pc_end_on
import com.storyteller_f.multi_core.StoppableTask
import com.storyteller_f.sort_ui.SortChain
import com.storyteller_f.sort_ui.SortDialog
import com.storyteller_f.ui_list.adapter.SimpleSourceAdapter
import com.storyteller_f.ui_list.core.BindingViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.data.SimpleResponse
import com.storyteller_f.ui_list.event.findActionReceiverOrNull
import com.storyteller_f.ui_list.source.SearchProducer
import com.storyteller_f.ui_list.source.SimpleSearchViewModel
import com.storyteller_f.ui_list.source.observerInScope
import com.storyteller_f.ui_list.source.search
import com.storyteller_f.ui_list.ui.ListWithState
import com.storyteller_f.ui_list.ui.toggle
import com.storyteller_f.ui_list.ui.valueContains
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.coroutines.resumeWithException

suspend fun getFileInstanceAsync(context: Context, uri: Uri) = suspendCancellableCoroutine {
    thread {
        val result = Result.success(getFileInstance(context, uri, it.stoppable()))
        it.resumeWith(result)
    }
}

fun getFileInstance(
    context: Context,
    uri: Uri,
    stoppableTask: StoppableTask = StoppableTask.Blocking
): FileInstance {
    val r = RootAccessFileInstance.remote
    val scheme = uri.scheme!!
    return when {
        scheme == RootAccessFileInstance.rootFileSystemScheme && r != null -> RootAccessFileInstance(
            r,
            uri
        )

        scheme == "ftp" -> FtpFileInstance(RemoteSpec.parse(uri), uri)
        scheme == "smb" -> SmbFileInstance(ShareSpec.parse(uri), uri)
        scheme == "sftp" -> SFtpFileInstance(RemoteSpec.parse(uri), uri)
        scheme == "ftpes" || scheme == "ftps" -> FtpsFileInstance(RemoteSpec.parse(uri), uri)
        scheme == "webdav" -> WebDavFileInstance(ShareSpec.parse(uri), uri)
        else -> FileInstanceFactory.getFileInstance(context, uri, stoppableTask)
    }
}

class FileListObserver<T>(
    private val owner: T,
    args: () -> FileListFragmentArgs,
    val scope: VMScope
) where T : ViewModelStoreOwner, T : SavedStateRegistryOwner {
    val fileInstance: FileInstance?
        get() = session.fileInstance.value
    val selected: List<Pair<DataItemHolder, Int>>?
        get() = session.selected.value
    val filters by keyPrefix({ "test" }, owner.svm({}, scope) { it, _ ->
        StateValueModel(it, default = listOf<Filter<FileSystemItemModel>>())
    })
    val sort by keyPrefix({ "sort" }, owner.svm({}, scope) { it, _ ->
        StateValueModel(it, default = listOf<SortChain<FileSystemItemModel>>())
    })
    val filterHiddenFile by owner.svm({}, scope) { it, _ ->
        StateValueModel(it, FileListFragment.filterHiddenFileKey, false)
    }
    private val displayGrid by keyPrefix("display", owner.vm({}, scope) { _ ->
        genericValueModel(false)
    })

    private val session by owner.vm(args) {
        FileExplorerSession(
            when (owner) {
                is Fragment -> owner.requireActivity().application
                is ComponentActivity -> owner.application
                else -> throw Exception("unrecognized ${owner.javaClass}")
            }, it.uri
        )
    }

    private val data by owner.search(
        {
            when (owner) {
                is Fragment -> owner.requireDatabase
                is Context -> owner.requireDatabase
                else -> throw Exception("unrecognized ${owner.javaClass}")
            } to session.selected
        },
        { (database, selected) ->
            SearchProducer(fileServiceBuilder(database)) { fileModel, _, sq ->
                FileItemHolder(fileModel, selected.value.orEmpty(), sq.display)
            }
        })

    fun setup(
        listWithState: ListWithState,
        adapter: SimpleSourceAdapter<FileItemHolder, FileViewHolder>,
        rightSwipe: (FileItemHolder) -> Unit,
        updatePath: (String) -> Unit
    ) {
        with(owner) {
            setup(listWithState, adapter, rightSwipe, updatePath)
        }

    }

    private fun T.setup(
        listWithState: ListWithState,
        adapter: SimpleSourceAdapter<FileItemHolder, FileViewHolder>,
        rightSwipe: (FileItemHolder) -> Unit,
        updatePath: (String) -> Unit
    ) {

        displayGrid.data.observe(owner) {
            listWithState.recyclerView.isVisible = false
            adapter.submitData(cycle, PagingData.empty())
            listWithState.recyclerView.layoutManager = when {
                it -> GridLayoutManager(listWithState.context, 3)
                else -> LinearLayoutManager(listWithState.context)
            }
        }
        fileList(
            listWithState,
            adapter,
            data,
            session,
            filterHiddenFile.data,
            filters.data,
            sort.data,
            displayGrid.data,
            rightSwipe, updatePath
        )
    }

    fun update(toParent: FileInstance) {
        session.fileInstance.value = toParent
    }
}

fun LifecycleOwner.fileList(
    listWithState: ListWithState,
    adapter: SimpleSourceAdapter<FileItemHolder, FileViewHolder>,
    viewModel: SimpleSearchViewModel<FileModel, FileExplorerSearch, FileItemHolder>,
    session: FileExplorerSession,
    filterHiddenFileLiveData: LiveData<Boolean>,
    filtersLiveData: LiveData<List<Filter<FileSystemItemModel>>>,
    sortLivedata: LiveData<List<SortChain<FileSystemItemModel>>>,
    display: LiveData<Boolean>,
    rightSwipe: (FileItemHolder) -> Unit,
    updatePath: (String) -> Unit
) {
    val owner = if (this is Fragment) viewLifecycleOwner else this
    context {
        listWithState.sourceUp(
            adapter,
            owner,
            plugLayoutManager = false,
            dampingSwipe = { viewHolder, direction ->
                if (direction == ItemTouchHelper.LEFT)
                    session.selected.toggle(viewHolder)
                else rightSwipe(viewHolder.itemHolder as FileItemHolder)
            },
            flash = ListWithState.Companion::remote
        )
        session.fileInstance.observe(owner) {
            updatePath(it.path)
        }
        combineDao(
            session.fileInstance,
            filterHiddenFileLiveData,
            filtersLiveData.same,
            sortLivedata.same,
            display
        ).observe(owner, Observer {
            val fileInstance = it.d1 ?: return@Observer
            val filterHiddenFile = it.d2 ?: return@Observer
            val filters = it.d3 ?: return@Observer
            val sortChains = it.d4 ?: return@Observer
            val path = fileInstance.uri
            //检查权限
            owner.lifecycleScope.launch {
                if (!checkPathPermission(path)) {
                    if (requestPermissionForSpecialPath(path)) {
                        adapter.refresh()
                    }
                }
            }

            viewModel.observerInScope(
                owner,
                FileExplorerSearch(
                    fileInstance,
                    filterHiddenFile,
                    filters,
                    sortChains,
                    if (it.d5 == true) "grid" else ""
                )
            ) { pagingData ->
                adapter.submitData(pagingData)
            }
        })

    }
}

private val <T> LiveData<List<T>>.same
    get() = distinctUntilChangedBy { sort1, sort2 ->
        sort1.same(sort2)
    }

class FileItemHolder(
    val file: FileModel,
    val selected: List<Pair<DataItemHolder, Int>>,
    override val variant: String
) : DataItemHolder {
    override fun areItemsTheSame(other: DataItemHolder) =
        (other as FileItemHolder).file.fullPath == file.fullPath

    override fun areContentsTheSame(other: DataItemHolder): Boolean {
        return (other as FileItemHolder).file == file
    }

}

@BindItemHolder(FileItemHolder::class, type = "grid")
class FileGridViewHolder(private val binding: ViewHolderFileGridBinding) :
    BindingViewHolder<FileItemHolder>(binding) {
    override fun bindData(itemHolder: FileItemHolder) {
        binding.fileName.text = itemHolder.file.name
        binding.fileIcon.fileIcon(itemHolder.file.item)
        binding.symLink.isVisible = itemHolder.file.isSymLink
    }

}

@BindItemHolder(FileItemHolder::class)
class FileViewHolder(private val binding: ViewHolderFileBinding) :
    BindingViewHolder<FileItemHolder>(binding) {
    override fun bindData(itemHolder: FileItemHolder) {
        binding.fileName.text = itemHolder.file.name
        binding.fileIcon.fileIcon(itemHolder.file.item)
        val item = itemHolder.file.item
        binding.root.isSelected = itemHolder.selected.valueContains(
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

        binding.torrentName.setVisible(
            item,
            { it: TorrentFileItemModel -> it.torrentName.valid() }) { it, i ->
            it.text = i.torrentName
        }

        binding.modifiedTime.setVisible(item.lastModifiedTime > 0 && item.formattedLastModifiedTime.valid()) {
            it.text = item.formattedLastModifiedTime
        }

        binding.detail.text = item.permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            itemHolder.file.item.dragSupport(binding.root)
        }
        binding.symLink.isVisible = itemHolder.file.isSymLink

    }


}

@RequiresApi(Build.VERSION_CODES.N)
private fun FileSystemItemModel.dragSupport(root: ConstraintLayout) {
    DragStartHelper(root) { view: View, _: DragStartHelper ->
        val clipData = ClipData.newPlainText(FileListFragment.clipDataKey, uri.toString())
        val flags = View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
        view.startDragAndDrop(clipData, View.DragShadowBuilder(view), null, flags)
    }.apply {
        attach()
    }
    root.setOnDragListener(if (!isDirectory) null
    else
        { v, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    val clipDescription = event.clipDescription
                    clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                            && clipDescription.label == FileListFragment.clipDataKey
                }

                DragEvent.ACTION_DROP -> {
                    v.findActionReceiverOrNull<FileListFragment>()
                        ?.pasteFiles(event.clipData, uri)
                    true
                }

                else -> true
            }
        })
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

class FileExplorerSearch(
    val path: FileInstance,
    val filterHiddenFile: Boolean,
    val filters: List<Filter<FileSystemItemModel>>,
    val sort: List<SortChain<FileSystemItemModel>>,
    val display: String
)

fun fileServiceBuilder(
    database: LocalDatabase
): suspend (searchQuery: FileExplorerSearch, start: Int, count: Int) -> SimpleResponse<FileModel> {
    return { searchQuery: FileExplorerSearch, start: Int, count: Int ->
        val listSafe = suspendCancellableCoroutine { continuation ->
            thread {
                try {
                    searchQuery.path.list().run {
                        continuation.resumeWith(Result.success(this))
                    }
                } catch (e: Exception) {
                    continuation.resumeWithException(e)
                }

            }
        }

        val filterList = searchQuery.filters
        val sortChains = searchQuery.sort

        val filterPredicate: (FileSystemItemModel) -> Boolean = {
            (!searchQuery.filterHiddenFile || !it.isHidden) && (filterList.isEmpty() || filterList.any { f ->
                f.filter(it)
            })
        }
        val directories = listSafe.directories.toList()
        val files = listSafe.files.toList()

        if (sortChains.isNotEmpty()) {
            SortDialog.sortInternal(directories, sortChains)
            SortDialog.sortInternal(files, sortChains)
        }
        val listFiles = if (searchQuery.filterHiddenFile || filterList.isNotEmpty()) {
            directories.filter(filterPredicate).plus(files.filter(filterPredicate))
        } else directories.plus(files)
        val total = listFiles.size
        val index = start - 1
        val startPosition = index * count
        if (startPosition > total) SimpleResponse(0)
        else {
            val items = listFiles
                .subList(startPosition, (startPosition + count).coerceAtMost(total))
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
    database: LocalDatabase
): FileModel {
    val length = if (model is FileItemModel) {
        database.mdDao().search(model.uri)?.let {
            if (it.lastUpdateTime > model.lastModifiedTime) model.md = it.data
        }
        if (model is TorrentFileItemModel)
            database.torrentDao().search(model.uri)?.let {
                if (it.lastUpdateTime > model.lastModifiedTime) model.torrentName = it.torrent
            }
        model.size
    } else {
        //从数据库中查找
        val directory = database.sizeDao().search(model.uri)
        if (directory != null && directory.lastUpdateTime > model.lastModifiedTime) {
            directory.size
        } else -1
    }
    model.formattedSize = format1024(length)
    model.size = length
    return FileModel(model.name, model.fullPath, length, model.isHidden, model, model.isSymLink)
}
