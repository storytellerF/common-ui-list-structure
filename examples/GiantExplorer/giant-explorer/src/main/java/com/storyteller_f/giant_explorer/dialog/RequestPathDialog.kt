package com.storyteller_f.giant_explorer.dialog

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.lifecycle.flowWithLifecycle
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.common_ui.*
import com.storyteller_f.common_vm_ktx.*
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system_ktx.isDirectory
import com.storyteller_f.giant_explorer.control.*
import com.storyteller_f.giant_explorer.databinding.DialogRequestPathBinding
import com.storyteller_f.giant_explorer.filter.FilterDialogManager
import com.storyteller_f.giant_explorer.view.PathMan
import com.storyteller_f.multi_core.StoppableTask
import com.storyteller_f.ui_list.adapter.SimpleSourceAdapter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class RequestPathDialog : SimpleDialogFragment<DialogRequestPathBinding>(DialogRequestPathBinding::inflate), RegistryFragment {
    private val dialogImpl = FilterDialogManager()
    private val observer = FileListObserver(this, {
        FileListFragmentArgs(FileInstanceFactory.rootUserEmulatedPath, FileInstanceFactory.publicFileSystemRoot)
    }, activityScope)

    @Parcelize
    class RequestPathResult(val path: String) : Parcelable

    private val adapter = SimpleSourceAdapter<FileItemHolder, FileViewHolder>(requestKey)

    companion object {
        const val requestKey = "request-path"
    }

    override fun onBindViewEvent(binding: DialogRequestPathBinding) {
        binding.bottom.requestScreenOn.setOnClick {
            it.keepScreenOn = it.isChecked
        }
        binding.bottom.positive.setOnClick {
            observer.fileInstance?.path?.let {
                setFragmentResult(RequestPathResult(it))
                dismiss()
            }
        }
        observer.filterHiddenFile .data.observe(viewLifecycleOwner) {
            binding.filterHiddenFile.isActivated = it
        }
        binding.filterHiddenFile.setOnClick {
            observer.filterHiddenFile.data.value = observer.filterHiddenFile.data.value?.not() ?: false
        }
        binding.bottom.negative.setOnClick {
            dismiss()
        }
        binding.newFile.setOnClick {
            dialog(NewNameDialog(), NewNameDialog.NewNameResult::class.java) { nameResult ->
                observer.fileInstance?.toChild(nameResult.name, false, true)
            }
        }
        (dialog as? ComponentDialog)?.onBackPressedDispatcher?.addCallback(this) {
            val value = observer.fileInstance
            if (value != null) {
                if (value.path == "/" || value.path == FileInstanceFactory.rootUserEmulatedPath) {
                    isEnabled = false
                    @Suppress("DEPRECATION") dialog?.onBackPressed()
                } else {
                    observer.update(FileInstanceFactory.toParent(value, requireContext(), StoppableTask.Blocking))
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
        dialogImpl.init(requireContext(), { observer.filters.data.value = it }, { observer.sort.data.value = it })
        observer.filters
        observer.sort
        observer.setup(binding.content, adapter, {}) {
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
                observer.update(getFileInstance(it, requireContext(), stoppableTask = StoppableTask.Blocking))
            }
        }
    }

    @BindClickEvent(FileItemHolder::class, viewName = "getRoot()", key = requestKey)
    fun toChild(itemHolder: FileItemHolder) {
        if (itemHolder.file.item.isDirectory) {
            val current = observer.fileInstance ?: return
            observer.update(FileInstanceFactory.toChild(
                current, itemHolder.file.name, false, requireContext(), false, StoppableTask.Blocking
            ))
        } else {
            setFragmentResult(RequestPathResult(itemHolder.file.fullPath))
            dismiss()
        }
    }

    override fun requestKey(): String {
        return requestKey
    }
}