package com.storyteller_f.giant_explorer.control

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.os.Build
import android.view.DragEvent
import android.view.View
import androidx.annotation.RequiresApi
import androidx.core.view.DragStartHelper
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.common_ktx.context
import com.storyteller_f.common_ktx.same
import com.storyteller_f.common_ui.setVisible
import com.storyteller_f.common_vm_ktx.combineDao
import com.storyteller_f.common_vm_ktx.distinctUntilChangedBy
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.checkPathPermission
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.file_system.model.TorrentFileItemModel
import com.storyteller_f.file_system.requestPermissionForSpecialPath
import com.storyteller_f.file_system_ktx.fileIcon
import com.storyteller_f.file_system_ktx.isDirectory
import com.storyteller_f.filter_core.Filter
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.control.plugin.stoppable
import com.storyteller_f.giant_explorer.database.LocalDatabase
import com.storyteller_f.giant_explorer.database.RemoteSpec
import com.storyteller_f.giant_explorer.database.ShareSpec
import com.storyteller_f.giant_explorer.databinding.ViewHolderFileBinding
import com.storyteller_f.giant_explorer.databinding.ViewHolderFileGridBinding
import com.storyteller_f.giant_explorer.model.FileModel
import com.storyteller_f.giant_explorer.pc_end_on
import com.storyteller_f.giant_explorer.service.FtpFileInstance
import com.storyteller_f.giant_explorer.service.FtpsFileInstance
import com.storyteller_f.giant_explorer.service.SFtpFileInstance
import com.storyteller_f.giant_explorer.service.SmbFileInstance
import com.storyteller_f.giant_explorer.service.WebDavFileInstance
import com.storyteller_f.multi_core.StoppableTask
import com.storyteller_f.sort_ui.SortChain
import com.storyteller_f.sort_ui.SortDialog
import com.storyteller_f.ui_list.adapter.SimpleSourceAdapter
import com.storyteller_f.ui_list.core.BindingViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.data.SimpleResponse
import com.storyteller_f.ui_list.event.findActionReceiverOrNull
import com.storyteller_f.ui_list.source.SimpleSearchViewModel
import com.storyteller_f.ui_list.source.observerInScope
import com.storyteller_f.ui_list.ui.ListWithState
import com.storyteller_f.ui_list.ui.toggle
import com.storyteller_f.ui_list.ui.valueContains
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.concurrent.thread
import kotlin.coroutines.resumeWithException

suspend fun getFileInstanceAsync(path: String, context: Context, root: String = FileInstanceFactory.publicFileSystemRoot) = suspendCancellableCoroutine {
    thread {
        val result = Result.success(getFileInstance(path, context, root, it.stoppable()))
        it.resumeWith(result)
    }
}

fun getFileInstance(path: String, context: Context, root: String = FileInstanceFactory.publicFileSystemRoot, stoppableTask: StoppableTask = StoppableTask.Blocking): FileInstance {
    return when {
        root.startsWith("ftp://") -> FtpFileInstance(path, root, RemoteSpec.parse(root))
        root.startsWith("smb://") -> SmbFileInstance(path, root, ShareSpec.parse(root))
        root.startsWith("sftp://") -> SFtpFileInstance(path, root, RemoteSpec.parse(root))
        root.startsWith("ftpes://") || root.startsWith("ftps://") -> FtpsFileInstance(path, root, RemoteSpec.parse(root))
        root.startsWith("webdav://") -> WebDavFileInstance(path, root, ShareSpec.parse(root))
        else -> FileInstanceFactory.getFileInstance(path, context, root, stoppableTask)
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
        listWithState.sourceUp(adapter, owner, plugLayoutManager = false, dampingSwipe = { viewHolder, direction ->
            if (direction == ItemTouchHelper.LEFT)
                session.selected.toggle(viewHolder)
            else rightSwipe(viewHolder.itemHolder as FileItemHolder)
        }, flash = ListWithState.Companion::remote)
        session.fileInstance.observe(owner) {
            updatePath(it.path)
        }
        combineDao(session.fileInstance, filterHiddenFileLiveData, filtersLiveData.same, sortLivedata.same, display).observe(owner, Observer {
            val fileInstance = it.d1 ?: return@Observer
            val filterHiddenFile = it.d2 ?: return@Observer
            val filters = it.d3 ?: return@Observer
            val sortChains = it.d4 ?: return@Observer
            val path = fileInstance.path
            //检查权限
            owner.lifecycleScope.launch {
                if (!checkPathPermission(path)) {
                    requestPermissionForSpecialPath(path)
                }
            }

            viewModel.observerInScope(owner, FileExplorerSearch(fileInstance, filterHiddenFile, filters, sortChains, if (it.d5 == true) "grid" else "")) { pagingData ->
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
    override val type: String
) : DataItemHolder {
    override fun areItemsTheSame(other: DataItemHolder) =
        (other as FileItemHolder).file.fullPath == file.fullPath

    override fun areContentsTheSame(other: DataItemHolder): Boolean {
        return (other as FileItemHolder).file == file
    }

}

@BindItemHolder(FileItemHolder::class, type = "grid")
class FileGridViewHolder(private val binding: ViewHolderFileGridBinding) : BindingViewHolder<FileItemHolder>(binding) {
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

class FileExplorerSearch(val path: FileInstance, val filterHiddenFile: Boolean, val filters: List<Filter<FileSystemItemModel>>, val sort: List<SortChain<FileSystemItemModel>>, val display: String)

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
    database: LocalDatabase
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
