package com.storyteller_f.giant_explorer.control

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.webkit.URLUtil
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.annotation_defination.BindLongClickEvent
import com.storyteller_f.common_ktx.mm
import com.storyteller_f.common_ui.SimpleFragment
import com.storyteller_f.common_ui.dialog
import com.storyteller_f.common_ui.fragment
import com.storyteller_f.common_ui.owner
import com.storyteller_f.common_vm_ktx.*
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system_ktx.isDirectory
import com.storyteller_f.giant_explorer.BuildConfig
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.FragmentFileListBinding
import com.storyteller_f.giant_explorer.dialog.*
import com.storyteller_f.ui_list.core.SearchProducer
import com.storyteller_f.ui_list.core.SimpleSourceAdapter
import com.storyteller_f.ui_list.core.search
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

class TempVM : ViewModel() {
    var list: MutableList<String> = mutableListOf()
    var dest: String? = null
}

class FileListFragment : SimpleFragment<FragmentFileListBinding>(FragmentFileListBinding::inflate) {
    private val fileOperateBinder
        get() = (requireContext() as MainActivity).fileOperateBinder
    private val uuid by kavm({ "uuid" }, {}) {
        GenericValueModel<String>().apply {
            data.value = UUID.randomUUID().toString()
        }
    }

    private val data by search({ requireDatabase to session.selected }, { (database, selected) ->
        SearchProducer(service(database)) { fileModel, _ ->
            FileItemHolder(fileModel, selected)
        }
    })

    private val filterHiddenFile by asvm({}) { it, _ ->
        HasStateValueModel(it, filterHiddenFileKey, false)
    }

    private val args by navArgs<FileListFragmentArgs>()

    private val session by vm({ args.path }) {
        FileExplorerSession(requireActivity().application, it)
    }
    private val temp by kpvm({ "temp" }, {}) {
        TempVM()
    }

    override fun onBindViewEvent(binding: FragmentFileListBinding) {
        val adapter = SimpleSourceAdapter<FileItemHolder, FileViewHolder>()
        supportDirectoryContent(
            binding.content, adapter, data, session,
            filterHiddenFile.data
        ) {
            (requireContext() as MainActivity).drawPath(it)
        }
        (requireActivity() as? MenuHost)?.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.file_list_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem) = when (menuItem.itemId) {
                 R.id.add_file -> {
                     findNavController().navigate(R.id.action_fileListFragment_to_newNameDialog)
                     fragment(NewNameDialog.requestKey) { nameResult: NewNameDialog.NewNameResult ->
                         session.fileInstance.value?.toChild(nameResult.name, true, true)
                     }
                     true
                 }
                 R.id.paste_file -> {
                     ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)?.let { manager ->
                         manager.primaryClip?.let { data ->
                             handleClipData(data)
                         }
                     }
                     true

                 }
                 R.id.background_task -> {
                     startActivity(Intent(requireContext(), BackgroundTaskConfigActivity::class.java))
                     true
                 }
                 else -> false
             }
        }, owner)
    }

    fun handleClipData(data: ClipData, path: String? = null) {
        val key = uuid.data.value ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            val dest = path?.let {
                FileInstanceFactory.getFileInstance(it, requireContext())
            } ?: session.fileInstance.value ?: kotlin.run {
                Toast.makeText(requireContext(), "无法确定目的地", Toast.LENGTH_LONG).show()
                return@launch
            }
            val mutableList = MutableList(data.itemCount) {
                data.getItemAt(it)
            }
            val regex = Regex("^/([\\w.]+/)*[\\w.]+$")
            val uriList = mutableList.mapNotNull {
                val text = it.coerceToText(requireContext()).toString()
                (if (URLUtil.isNetworkUrl(text)) Uri.parse(text)
                else if (regex.matches(text)) {
                    Uri.fromFile(File(text))
                } else {
                    Toast.makeText(requireContext(), "正则失败$text", Toast.LENGTH_LONG).show()
                    null
                })?.takeIf { uri -> uri.toString().isNotEmpty() }
            }.plus(mutableList.mapNotNull {
                it.uri
            })
            temp.list.clear()
            temp.list.addAll(uriList.map { it.toString() })
            temp.dest = dest.path
            val fileOperateBinderLocal = fileOperateBinder ?: kotlin.run {
                Toast.makeText(requireContext(), "未连接服务", Toast.LENGTH_LONG).show()
                return@launch
            }
            if (activity?.getSharedPreferences("${requireContext().packageName}_preferences", Activity.MODE_PRIVATE)?.getBoolean("notify_before_paste", true) == true) {
                dialog(TaskConfirmDialog()) { r: TaskConfirmDialog.Result ->
                    if (r.confirm)
                        fileOperateBinderLocal.compoundTask(uriList, dest, key)
                }
            } else {
                fileOperateBinderLocal.compoundTask(uriList, dest, key)
            }
        }

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
                    val uriForFile = FileProvider.getUriForFile(requireContext(), BuildConfig.File_PROVIDER_AUTHORITY, file)
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
                            val apply = ClipData.newPlainText(clipDataKey, map.first().toString()).apply {
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
        const val filterHiddenFileKey = "filter-hidden-file"
        const val clipDataKey = "file explorer"
    }

    private fun detectSelected(itemHolder: FileItemHolder) =
        session.selected.value?.map { pair -> (pair.first as FileItemHolder).file.item } ?: listOf(itemHolder.file.item)

    override fun requestKey() = "file-list"

}