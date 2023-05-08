package com.storyteller_f.giant_explorer.dialog

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.flowWithLifecycle
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.common_ui.*
import com.storyteller_f.common_vm_ktx.*
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system_ktx.isDirectory
import com.storyteller_f.giant_explorer.control.*
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.DialogRequestPathBinding
import com.storyteller_f.giant_explorer.filter.FilterDialogManager
import com.storyteller_f.giant_explorer.filter.buildFilterDialogState
import com.storyteller_f.giant_explorer.filter.buildSortDialogState
import com.storyteller_f.giant_explorer.view.PathMan
import com.storyteller_f.multi_core.StoppableTask
import com.storyteller_f.ui_list.adapter.SimpleSourceAdapter
import com.storyteller_f.ui_list.source.SearchProducer
import com.storyteller_f.ui_list.source.search
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class RequestPathDialog : SimpleDialogFragment<DialogRequestPathBinding>(DialogRequestPathBinding::inflate), RegistryFragment {
    private val session by vm({ FileInstanceFactory.rootUserEmulatedPath }) {
        FileExplorerSession(requireActivity().application, it, FileInstanceFactory.publicFileSystemRoot)
    }
    private val filterHiddenFile by svm({}) { it, _ ->
        StateValueModel(it, FileListFragment.filterHiddenFileKey, false)
    }
    private val filters by keyPrefix({ "filter" }, svm({ dialogImpl.filterDialog }, vmProducer = buildFilterDialogState))
    private val sort by keyPrefix({ "sort" }, svm({ dialogImpl.sortDialog }, vmProducer = buildSortDialogState))
    private val dialogImpl = FilterDialogManager()

    @Parcelize
    class RequestPathResult(val path: String) : Parcelable

    private val data by search({ requireDatabase }, {
        SearchProducer(fileServiceBuilder(it)) { fileModel, _, sq ->
            FileItemHolder(fileModel, mutableListOf(), sq.display)
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
            binding.filterHiddenFile.isActivated = it
        }
        binding.filterHiddenFile.setOnClick {
            filterHiddenFile.data.value = filterHiddenFile.data.value?.not() ?: false
        }
        binding.bottom.negative.setOnClick {
            dismiss()
        }
        binding.newFile.setOnClick {
            dialog(NewNameDialog(), NewNameDialog.NewNameResult::class.java) { nameResult ->
                session.fileInstance.value?.toChild(nameResult.name, false, true)
            }
        }
        (dialog as? ComponentDialog)?.onBackPressedDispatcher?.addCallback(this) {
            val value = session.fileInstance.value
            if (value != null) {
                if (value.path == "/" || value.path == FileInstanceFactory.rootUserEmulatedPath) {
                    isEnabled = false
                    @Suppress("DEPRECATION") dialog?.onBackPressed()
                } else {
                    session.fileInstance.value = FileInstanceFactory.toParent(value, requireContext(), StoppableTask.Blocking)
                }
            }
        }
        binding.filter.setOnClick {
            dialogImpl.showFilter()
        }
        binding.sort.setOnClick {
            dialogImpl.showSort()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialogImpl.init(requireContext(), { filters.data.value = it }, { sort.data.value = it })
        filters
        sort
        fileList(binding.content, adapter, data, session, filterHiddenFile.data, filters.data, sort.data, MutableLiveData(false), {

        }) {
            binding.pathMan.drawPath(it)
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
                session.fileInstance.value = getFileInstance(it, requireContext(), stoppableTask = StoppableTask.Blocking)
            }
        }
    }

    @BindClickEvent(FileItemHolder::class, viewName = "getRoot()", key = requestKey)
    fun toChild(itemHolder: FileItemHolder) {
        if (itemHolder.file.item.isDirectory) {
            val current = session.fileInstance.value ?: return
            session.fileInstance.value = FileInstanceFactory.toChild(
                current, itemHolder.file.name, false, requireContext(), false, StoppableTask.Blocking
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