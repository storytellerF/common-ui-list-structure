package com.storyteller_f.giant_explorer.dialog

import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.activity.ComponentDialog
import androidx.activity.addCallback
import androidx.core.net.toUri
import androidx.lifecycle.flowWithLifecycle
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.common_ui.*
import com.storyteller_f.common_vm_ktx.*
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.FileCreatePolicy
import com.storyteller_f.file_system_ktx.isDirectory
import com.storyteller_f.giant_explorer.control.*
import com.storyteller_f.giant_explorer.databinding.DialogRequestPathBinding
import com.storyteller_f.multi_core.StoppableTask
import com.storyteller_f.ui_list.adapter.SimpleSourceAdapter
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.io.File

class RequestPathDialog :
    SimpleDialogFragment<DialogRequestPathBinding>(DialogRequestPathBinding::inflate),
    Registry {
    private val observer = FileListObserver(this, {
        FileListFragmentArgs(File(FileInstanceFactory.rootUserEmulatedPath).toUri())
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
        observer.filterHiddenFile.data.observe(viewLifecycleOwner) {
            binding.filterHiddenFile.isActivated = it
        }
        binding.filterHiddenFile.setOnClick {
            observer.filterHiddenFile.data.value =
                observer.filterHiddenFile.data.value?.not() ?: false
        }
        binding.bottom.negative.setOnClick {
            dismiss()
        }
        binding.newFile.setOnClick {
            val requestDialog = request(NewNameDialog::class.java)
            observeResponse(requestDialog, NewNameDialog.NewNameResult::class.java) { nameResult ->
                observer.fileInstance?.toChild(nameResult.name, FileCreatePolicy.Create(false))
            }
        }
        (dialog as? ComponentDialog)?.onBackPressedDispatcher?.addCallback(this) {
            val value = observer.fileInstance
            if (value != null) {
                if (value.path == "/" || value.path == FileInstanceFactory.rootUserEmulatedPath) {
                    isEnabled = false
                    @Suppress("DEPRECATION") dialog?.onBackPressed()
                } else {
                    observer.update(
                        FileInstanceFactory.toParent(
                            requireContext(),
                            value,
                            StoppableTask.Blocking
                        )
                    )
                }
            }
        }
        binding.filter.setOnClick {
            request(FilterDialogFragment::class.java)
        }
        binding.sort.setOnClick {
            request(SortDialogFragment::class.java)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observer.setup(binding.content, adapter, {}) {
            binding.pathMan.drawPath(it)
        }
        scope.launch {
            callbackFlow {
                binding.pathMan.setPathChangeListener { pathString -> trySend(pathString) }
                awaitClose {
                    binding.pathMan.setPathChangeListener(null)
                }
            }.flowWithLifecycle(lifecycle).collectLatest {
                val uri = observer.fileInstance?.uri?.buildUpon()?.path(it)?.build()
                    ?: return@collectLatest
                observer.update(
                    getFileInstance(
                        requireContext(),
                        uri,
                        stoppableTask = StoppableTask.Blocking
                    )
                )
            }
        }
    }

    @BindClickEvent(FileItemHolder::class, viewName = "getRoot()", key = requestKey)
    fun toChild(itemHolder: FileItemHolder) {
        if (itemHolder.file.item.isDirectory) {
            val current = observer.fileInstance ?: return
            observer.update(
                FileInstanceFactory.toChild(
                    requireContext(),
                    current,
                    itemHolder.file.name,
                    FileCreatePolicy.NotCreate,
                    StoppableTask.Blocking
                )
            )
        } else {
            setFragmentResult(RequestPathResult(itemHolder.file.fullPath))
            dismiss()
        }
    }

}