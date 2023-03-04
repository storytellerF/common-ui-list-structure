package com.storyteller_f.giant_explorer.dialog

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.flowWithLifecycle
import androidx.paging.PagingData
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.common_ktx.context
import com.storyteller_f.common_ui.*
import com.storyteller_f.common_vm_ktx.*
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.checkPathPermission
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.file_system.requestPermissionForSpecialPath
import com.storyteller_f.file_system_ktx.isDirectory
import com.storyteller_f.filter_core.Filter
import com.storyteller_f.giant_explorer.control.*
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.DialogRequestPathBinding
import com.storyteller_f.giant_explorer.view.PathMan
import com.storyteller_f.sort_ui.SortChain
import com.storyteller_f.ui_list.adapter.SimpleSourceAdapter
import com.storyteller_f.ui_list.source.SearchProducer
import com.storyteller_f.ui_list.source.observerInScope
import com.storyteller_f.ui_list.source.search
import com.storyteller_f.ui_list.ui.ListWithState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class RequestPathDialog :
    SimpleDialogFragment<DialogRequestPathBinding>(DialogRequestPathBinding::inflate), RegistryFragment {
    private val session by vm({ FileInstanceFactory.rootUserEmulatedPath }) {
        FileExplorerSession(requireActivity().application, it, FileInstanceFactory.publicFileSystemRoot)
    }
    private val filterHiddenFile by svm({}) { it, _ ->
        StateValueModel(it, FileListFragment.filterHiddenFileKey, false)
    }
    private val filters by keyPrefix({ "test" }, svm({}) { it, _ ->
        StateValueModel(it, default = mutableListOf<Filter<FileSystemItemModel>>())
    })
    private val sort by keyPrefix({ "sort" }, svm({}) { it, f ->
        StateValueModel(it, default = mutableListOf<SortChain<FileSystemItemModel>>())
    })

    @Parcelize
    class RequestPathResult(val path: String) : Parcelable

    private val data by search({ requireDatabase }, {
        SearchProducer(fileServiceBuilder(it)) { fileModel, _ ->
            FileItemHolder(fileModel, MutableLiveData(mutableListOf()))
        }
    }

    )
    private val adapter = SimpleSourceAdapter<FileItemHolder, FileViewHolder>(requestKey)

    companion object {
        const val requestKey = "request-path"
    }

    override fun onBindViewEvent(binding: DialogRequestPathBinding) {
        binding.bottom.requestScreenOn.setOnClick {
            it.keepScreenOn = it.isChecked
        }
        binding.bottom.positive.setOnClick {
            session.fileInstance.value?.path?.let {
                setFragmentResult(RequestPathResult(it))
                dismiss()
            }
        }
        filterHiddenFile.data.observe(viewLifecycleOwner) {
            binding.filterHiddenFile.isChecked = it
        }
        binding.filterHiddenFile.setOnCheckedChangeListener { _, isChecked ->
            filterHiddenFile.data.value = isChecked
        }
        binding.bottom.negative.setOnClick {
            dismiss()
        }
        binding.newFile.setOnClick {
            dialog(NewNameDialog()) { nameResult: NewNameDialog.NewNameResult ->
                session.fileInstance.value?.toChild(nameResult.name, true, true)
            }
        }
        (dialog as? ComponentDialog)?.onBackPressedDispatcher?.addCallback(this) {
            val value = session.fileInstance.value
            if (value != null) {
                if (value.path == "/" || value.path == FileInstanceFactory.rootUserEmulatedPath) {
                    isEnabled = false
                    @Suppress("DEPRECATION")
                    dialog?.onBackPressed()
                } else {
                    session.fileInstance.value = FileInstanceFactory.toParent(value, requireContext())
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context {
            binding.content.sourceUp(adapter, owner, session.selected, flash = ListWithState.Companion::remote)
            session.fileInstance.observe(owner) {
                binding.pathMan.drawPath(it.path)
            }
            combine("file" to session.fileInstance, "filter" to filterHiddenFile.data, "filters" to filters.data, "sort" to sort.data).observe(owner, Observer {
                val filter = it["filter"] as? Boolean ?: return@Observer
                val filters = it["filters"] as? MutableList<Filter<FileSystemItemModel>> ?: return@Observer
                val file = it["file"] as FileInstance? ?: return@Observer
                val sort = it["sort"] as? MutableList<SortChain<FileSystemItemModel>> ?: return@Observer
                val path = file.path
                //检查权限
                scope.launch {
                    if (!checkPathPermission(path)) {
                        requestPermissionForSpecialPath(path)
                    }
                }

                data.observerInScope(owner, FileExplorerSearch(file, filter, filters, sort)) { pagingData ->
                    adapter.submitData(PagingData.empty())
                    delay(100)
                    adapter.submitData(pagingData)
                }
            })

        }
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
                session.fileInstance.value = getFileInstance(it, requireContext())
            }
        }
    }

    @BindClickEvent(FileItemHolder::class, viewName = "getRoot()", key = requestKey)
    fun toChild(itemHolder: FileItemHolder) {
        if (itemHolder.file.item.isDirectory) {
            val current = session.fileInstance.value ?: return
            session.fileInstance.value = FileInstanceFactory.toChild(
                current,
                itemHolder.file.name,
                false,
                requireContext(),
                false
            )
        } else {
            setFragmentResult(RequestPathResult(itemHolder.file.fullPath))
            dismiss()
        }
    }

    override fun requestKey(): String {
        return requestKey
    }
}