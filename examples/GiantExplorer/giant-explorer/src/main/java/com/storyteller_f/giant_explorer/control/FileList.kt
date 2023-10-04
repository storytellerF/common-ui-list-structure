package com.storyteller_f.giant_explorer.control

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.os.Build
import android.view.DragEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.DragStartHelper
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.distinctUntilChanged
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
import com.storyteller_f.common_vm_ktx.VMScope
import com.storyteller_f.common_vm_ktx.combineDao
import com.storyteller_f.common_vm_ktx.debounce
import com.storyteller_f.common_vm_ktx.distinctUntilChangedBy
import com.storyteller_f.common_vm_ktx.svm
import com.storyteller_f.common_vm_ktx.vm
import com.storyteller_f.common_vm_ktx.wait5
import com.storyteller_f.file_system.checkPathPermission
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.file_system.model.TorrentFileItemModel
import com.storyteller_f.file_system.requestPathPermission
import com.storyteller_f.file_system_ktx.fileIcon
import com.storyteller_f.file_system_ktx.isDirectory
import com.storyteller_f.filter_core.Filter
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.database.LocalDatabase
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.ViewHolderFileBinding
import com.storyteller_f.giant_explorer.databinding.ViewHolderFileGridBinding
import com.storyteller_f.giant_explorer.dialog.activeFilters
import com.storyteller_f.giant_explorer.dialog.activeSortChains
import com.storyteller_f.giant_explorer.model.FileModel
import com.storyteller_f.giant_explorer.pc_end_on
import com.storyteller_f.sort_core.config.SortChain
import com.storyteller_f.sort_core.config.SortChains
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
import java.util.Locale

class FileListViewModel(stateHandle: SavedStateHandle): ViewModel() {
    val displayGrid = stateHandle.getLiveData("display", false)
    val filterHiddenFile = stateHandle.getLiveData("filterHiddenFile", false)
}

/**
 * @param owner 一般来说owner 都是this
 * @param scope viewModel 作用域
 */
class FileListObserver<T>(
    private val owner: T,
    args: () -> FileListFragmentArgs,
    val scope: VMScope
) where T : ViewModelStoreOwner, T : SavedStateRegistryOwner {
    val fileInstance: FileInstance?
        get() = session.fileInstance.value
    val selected: List<Pair<DataItemHolder, Int>>?
        get() = session.selected.value

    val fileListViewModel by owner.svm({}, scope) { it, _ ->
        FileListViewModel(it)
    }

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

        fileListViewModel.displayGrid.observe(owner) {
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
            fileListViewModel,
            rightSwipe,
            updatePath
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
    fileListViewModel: FileListViewModel,
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
        session.fileInstance.observe(owner) {
            val path = it.uri
            //检查权限
            owner.lifecycleScope.launch {
                if (!checkPathPermission(path)) {
                    if (requestPathPermission(path)) {
                        adapter.refresh()
                    }
                }
            }
        }
        combineDao(
            session.fileInstance,
            fileListViewModel.filterHiddenFile,
            activeFilters.same,
            activeSortChains.same,
            fileListViewModel.displayGrid
        ).wait5().distinctUntilChanged().debounce(200)
            .observe(owner) { (fileInstance, filterHiddenFile, filters, sortChains, d5) ->
                val display = if (d5) "grid" else ""

                viewModel.observerInScope(
                    owner,
                    FileExplorerSearch(
                        fileInstance,
                        filterHiddenFile,
                        filters,
                        sortChains,
                        display
                    )
                ) { pagingData ->
                    adapter.submitData(pagingData)
                }
            }

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
        val clipData = ClipData.newPlainText(FileListFragment.CLIP_DATA_KEY, uri.toString())
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
                            && clipDescription.label == FileListFragment.CLIP_DATA_KEY
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
        val listSafe = searchQuery.path.list()

        val filterList = searchQuery.filters
        val sortChains = searchQuery.sort

        val filterPredicate: (FileSystemItemModel) -> Boolean = {
            (!searchQuery.filterHiddenFile || !it.isHidden) && (filterList.isEmpty() || filterList.any { f ->
                f.filter(it)
            })
        }
        val directories = listSafe.directories
        val files = listSafe.files

        if (sortChains.isNotEmpty()) {
            val sortChains1 = SortChains(sortChains)
            directories.sortWith(sortChains1)
            files.sortWith(sortChains1)
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
