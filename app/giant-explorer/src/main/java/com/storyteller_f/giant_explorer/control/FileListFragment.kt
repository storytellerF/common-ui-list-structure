package com.storyteller_f.giant_explorer.control

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.paging.PagingData
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.annotation_defination.BindLongClickEvent
import com.storyteller_f.common_ktx.mm
import com.storyteller_f.common_ui.CommonFragment
import com.storyteller_f.common_ui.scope
import com.storyteller_f.common_vm_ktx.HasStateValueModel
import com.storyteller_f.common_vm_ktx.asvm
import com.storyteller_f.common_vm_ktx.toDiff
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.file_system.model.FilesAndDirectories
import com.storyteller_f.file_system.model.TorrentFileModel
import com.storyteller_f.file_system_ktx.isDirectory
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.FragmentFileListBinding
import com.storyteller_f.giant_explorer.dialog.NewNameDialog
import com.storyteller_f.giant_explorer.dialog.OpenFileDialog
import com.storyteller_f.giant_explorer.dialog.OpenFileDialogArgs
import com.storyteller_f.giant_explorer.dialog.RequestPathDialog
import com.storyteller_f.giant_explorer.model.FileModel
import com.storyteller_f.ui_list.core.SearchProducer
import com.storyteller_f.ui_list.core.SimpleSourceAdapter
import com.storyteller_f.ui_list.core.search
import com.storyteller_f.ui_list.data.SimpleResponse
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.min

class FileListFragment : CommonFragment<FragmentFileListBinding>(FragmentFileListBinding::inflate) {
    private val fileOperateBinder
        get() = (requireContext() as MainActivity).fileOperateBinder
    private val data by search({ requireDatabase() to session.selected }, {(database, selected)->
        SearchProducer(service(database)) { fileModel, _ ->
            FileItemHolder(fileModel, selected)
        }
    }

    )
    private val filterHiddenFile by asvm {
        HasStateValueModel(it, "filter-hidden-file", false)
    }

    private val args by navArgs<FileListFragmentArgs>()

    private val session by viewModels<FileExplorerSession>()

    override fun onBindViewEvent(binding: FragmentFileListBinding) {
        val adapter = SimpleSourceAdapter<FileItemHolder, FileViewHolder>()
        supportDirectoryContent(
            binding.content, adapter, data, session,
            filterHiddenFile.data
        ) {
            (requireContext() as MainActivity).drawPath(it)
        }
        session.selected.toDiff().observe(this) {
        }

        session.init(this, args.path)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.file_list_menu, menu)
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_file -> {
                findNavController().navigate(R.id.action_fileListFragment_to_newNameDialog)
                fragment<NewNameDialog.NewNameResult> { bundle ->
                    session.fileInstance.value?.toChild(bundle.name, true, true)
                }
            }

            R.id.background_task -> {
                startActivity(Intent(requireContext(), BackgroundTaskConfigActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }


    @BindClickEvent(FileItemHolder::class)
    fun toChild(itemHolder: FileItemHolder) {
        if (itemHolder.file.item.isDirectory) {
            val old = session.fileInstance.value ?: return
            findNavController().navigate(R.id.action_fileListFragment_self, FileListFragmentArgs(File(old.path, itemHolder.file.name).absolutePath).toBundle())
        } else {
            findNavController().navigate(R.id.action_fileListFragment_to_openFileDialog, OpenFileDialogArgs(itemHolder.file.fullPath).toBundle())
            fragment<OpenFileDialog.OpenFileResult> {
                Intent("android.intent.action.VIEW").apply {
                    addCategory("android.intent.category.DEFAULT")
                    val file = File(itemHolder.file.fullPath)
                    val uriForFile = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.file-provider", file)
                    setDataAndType(uriForFile, it.mimeType)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }.let {
                    startActivity(Intent.createChooser(it, "open by"))
                }
            }
        }
    }

    @BindLongClickEvent(FileItemHolder::class)
    fun test(view: View, itemHolder: FileItemHolder) {
        PopupMenu(requireContext(), view).apply {
            inflate(R.menu.item_context_menu)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.delete -> {
                        fileOperateBinder?.delete(itemHolder.file.item, listOf(itemHolder.file.item))
                    }
                    R.id.move_to -> {
                        moveOrCopy(true, itemHolder)
                    }
                    R.id.copy_to -> {
                        moveOrCopy(false, itemHolder)
                    }

                }
                true
            }
        }.show()
    }

    private fun moveOrCopy(move: Boolean, itemHolder: FileItemHolder) {
        RequestPathDialog().show(parentFragmentManager, RequestPathDialog.requestKey)
        fragment<RequestPathDialog.RequestPathResult> { result ->
            result.path.mm {
                FileInstanceFactory.getFileInstance(it, requireContext())
            }.mm { dest ->
                val detectSelected = detectSelected(itemHolder)
                fileOperateBinder?.moveOrCopy(dest, detectSelected, itemHolder.file.item, move)
            }
        }
    }

    companion object {
        private const val TAG = "FileListFragment"
    }

    private fun detectSelected(itemHolder: FileItemHolder) =
        session.selected.value?.map { pair -> (pair.first as FileItemHolder).file.item } ?: listOf(itemHolder.file.item)

    override fun requestKey() = "file-list"

}