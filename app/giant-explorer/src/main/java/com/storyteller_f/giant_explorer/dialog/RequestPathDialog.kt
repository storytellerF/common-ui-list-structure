package com.storyteller_f.giant_explorer.dialog

import android.os.Parcelable
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingData
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.common_ktx.context
import com.storyteller_f.common_ui.CommonDialogFragment
import com.storyteller_f.common_ui.scope
import com.storyteller_f.common_ui.setFragmentResult
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.common_vm_ktx.HasStateValueModel
import com.storyteller_f.common_vm_ktx.combine
import com.storyteller_f.common_vm_ktx.svm
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.checkPathPermission
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.requestPermissionForSpecialPath
import com.storyteller_f.file_system_ktx.isDirectory
import com.storyteller_f.giant_explorer.control.*
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.DialogRequestPathBinding
import com.storyteller_f.ui_list.core.SearchProducer
import com.storyteller_f.ui_list.core.SimpleSourceAdapter
import com.storyteller_f.ui_list.core.observerInScope
import com.storyteller_f.ui_list.core.search
import com.storyteller_f.ui_list.ui.ListWithState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class RequestPathDialog :
    CommonDialogFragment<DialogRequestPathBinding>(DialogRequestPathBinding::inflate) {
    private val session by viewModels<FileExplorerSession>()
    private val filterHiddenFile by svm {
        HasStateValueModel(it, "filter-hidden-file", false)
    }

    @Parcelize
    class RequestPathResult(val path: String) : Parcelable

    private val data by search({ requireDatabase() }, {
        SearchProducer(service(it)) { fileModel, _ ->
            FileItemHolder(fileModel, MutableLiveData(mutableListOf()))
        }
    }

    )
    private val adapter = SimpleSourceAdapter<FileItemHolder, FileViewHolder>()

    companion object {
        const val requestKey = "request-path"
    }

    override fun onBindViewEvent(binding: DialogRequestPathBinding) {
        binding.bottom.requestScreenOn.setOnClick {
            it.keepScreenOn = it.isChecked
        }
        binding.bottom.positive.setOnClick {
            session.fileInstance.value?.path?.let {
                setFragmentResult(requestKey, RequestPathResult(it))
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
        val owner = viewLifecycleOwner
        context {
            binding.content.sourceUp(adapter, owner, session.selected, flash = ListWithState.Companion::remote)
            session.fileInstance.observe(owner) {
                binding.pathMan.drawPath(it.path)
            }
            combine("file" to session.fileInstance, "filter" to filterHiddenFile.data).observe(owner, Observer {
                val filter = it["filter"] as? Boolean ?: return@Observer
                val file = it["file"] as FileInstance? ?: return@Observer
                val path = file.path
                //检查权限
                owner.lifecycleScope.launch {
                    if (!checkPathPermission(path)) {
                        requestPermissionForSpecialPath(path)
                    }
                }

                data.observerInScope(owner, FileExplorerSearch(file, filter)) { pagingData ->
                    adapter.submitData(PagingData.empty())
                    delay(100)
                    adapter.submitData(pagingData)
                }
            })

        }
        session.init(this, null)
        scope.launch {
            callbackFlow {
                binding.pathMan.setPathChangeListener {
                    trySend(it)
                }
                awaitClose {
                    binding.pathMan.setPathChangeListener(null)
                }
            }.flowWithLifecycle(lifecycle).collectLatest {
                session.fileInstance.value = FileInstanceFactory.getFileInstance(it, requireContext())
            }
        }

//        requireActivity().onBackPressedDispatcher.addCallback(this) {
//            val value = session.fileInstance.value
//            if (value != null) {
//                if (value.path == "/" || value.path == FileInstanceFactory.rootUserEmulatedPath) {
//                    isEnabled = false
//                    requireActivity().onBackPressed()
//                } else {
//                    session.fileInstance.value = FileInstanceFactory.toParent(value, requireContext())
//                }
//            }
//        }
    }

    @BindClickEvent(FileItemHolder::class)
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
        }
    }

    override fun requestKey(): String {
        return requestKey
    }
}