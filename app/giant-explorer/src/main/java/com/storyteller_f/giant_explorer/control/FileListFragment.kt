package com.storyteller_f.giant_explorer.control

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.webkit.URLUtil
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.annotation_defination.BindLongClickEvent
import com.storyteller_f.common_ktx.mm
import com.storyteller_f.common_ui.CommonFragment
import com.storyteller_f.common_ui.dialog
import com.storyteller_f.common_ui.fragment
import com.storyteller_f.common_vm_ktx.*
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system_ktx.isDirectory
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.FragmentFileListBinding
import com.storyteller_f.giant_explorer.dialog.NewNameDialog
import com.storyteller_f.giant_explorer.dialog.OpenFileDialog
import com.storyteller_f.giant_explorer.dialog.OpenFileDialogArgs
import com.storyteller_f.giant_explorer.dialog.RequestPathDialog
import com.storyteller_f.ui_list.core.SearchProducer
import com.storyteller_f.ui_list.core.SimpleSourceAdapter
import com.storyteller_f.ui_list.core.search
import java.io.File
import java.util.*

class FileListFragment : CommonFragment<FragmentFileListBinding>(FragmentFileListBinding::inflate) {
    private val fileOperateBinder
        get() = (requireContext() as MainActivity).fileOperateBinder
    private val uuid by kavm({ "uuid" }) {
        GenericValueModel<String>().apply {
            data.value = UUID.randomUUID().toString()
        }
    }
    private val data by search({ requireDatabase() to session.selected }, { (database, selected) ->
        SearchProducer(service(database)) { fileModel, _ ->
            FileItemHolder(fileModel, selected)
        }
    }

    )
    private val filterHiddenFile by asvm {
        HasStateValueModel(it, filterHiddenFileKey, false)
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
                fragment(NewNameDialog.requestKey) { nameResult: NewNameDialog.NewNameResult ->
                    session.fileInstance.value?.toChild(nameResult.name, true, true)
                }
            }
            R.id.paste_file -> {
                ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)?.let { manager ->
                    manager.primaryClip?.let { data ->
                        handleClipData(data)
                    }
                }

            }
            R.id.background_task -> {
                startActivity(Intent(requireContext(), BackgroundTaskConfigActivity::class.java))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun handleClipData(data: ClipData): Boolean {
        val key = uuid.data.value ?: return true
        val mutableList = MutableList(data.itemCount) {
            data.getItemAt(it)
        }
        val uriList = mutableList.mapNotNull {
            val toString = it.coerceToText(requireContext()).toString()
            (if (URLUtil.isNetworkUrl(toString)) Uri.parse(toString)
            else {
                Uri.fromFile(File(toString))
            }).takeIf { uri -> uri.toString().isNotEmpty() }
        }.plus(mutableList.mapNotNull {
            it.uri
        })
        session.fileInstance.value?.let {
            fileOperateBinder?.compoundTask(uriList, it, key)
        }
        return true
    }


    @BindClickEvent(FileItemHolder::class)
    fun toChild(itemHolder: FileItemHolder) {
        if (itemHolder.file.item.isDirectory) {
            val old = session.fileInstance.value ?: return
            findNavController().navigate(R.id.action_fileListFragment_self, FileListFragmentArgs(File(old.path, itemHolder.file.name).absolutePath).toBundle())
        } else {
            findNavController().navigate(R.id.action_fileListFragment_to_openFileDialog, OpenFileDialogArgs(itemHolder.file.fullPath).toBundle())
            fragment(OpenFileDialog.key) { r: OpenFileDialog.OpenFileResult ->
                Intent("android.intent.action.VIEW").apply {
                    addCategory("android.intent.category.DEFAULT")
                    val file = File(itemHolder.file.fullPath)
                    val uriForFile = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.file-provider", file)
                    setDataAndType(uriForFile, r.mimeType)
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
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.delete -> {
                        val key = uuid.data.value ?: return@setOnMenuItemClickListener true
                        fileOperateBinder?.delete(itemHolder.file.item, listOf(itemHolder.file.item), key)
                    }
                    R.id.move_to -> {
                        moveOrCopy(true, itemHolder)
                    }
                    R.id.copy_to -> {
                        moveOrCopy(false, itemHolder)
                    }
                    R.id.copy_file -> {
                        ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)?.let { manager ->
                            val map = detectSelected(itemHolder).map {
                                Uri.fromFile(File(it.fullPath))
                            }
                            val apply = ClipData.newPlainText("file explorer", map.first().toString()).apply {
                                if (map.size > 1) map.subList(1, map.size).forEach {
                                    addItem(ClipData.Item(it))
                                }
                            }
                            manager.setPrimaryClip(apply)
                        }
                    }

                }
                true
            }
        }.show()
    }

    private fun moveOrCopy(move: Boolean, itemHolder: FileItemHolder) {
        dialog(RequestPathDialog()) { result: RequestPathDialog.RequestPathResult ->
            result.path.mm {
                FileInstanceFactory.getFileInstance(it, requireContext())
            }.mm { dest ->
                val key = uuid.data.value ?: return@mm
                val detectSelected = detectSelected(itemHolder)
                fileOperateBinder?.moveOrCopy(dest, detectSelected, itemHolder.file.item, move, key)
            }
        }
    }

    companion object {
        private const val TAG = "FileListFragment"
        const val filterHiddenFileKey = "filter-hidden-file"
    }

    private fun detectSelected(itemHolder: FileItemHolder) =
        session.selected.value?.map { pair -> (pair.first as FileItemHolder).file.item } ?: listOf(itemHolder.file.item)

    override fun requestKey() = "file-list"

}