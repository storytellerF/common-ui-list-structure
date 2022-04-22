package com.storyteller_f.giant_explorer.dialog

import android.os.Parcelable
import androidx.lifecycle.MutableLiveData
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.common_ui.CommonDialogFragment
import com.storyteller_f.common_ui.setFragmentResult
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.common_vm_ktx.GenericValueModel
import com.storyteller_f.common_vm_ktx.vm
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system_ktx.isDirectory
import com.storyteller_f.giant_explorer.FileItemHolder
import com.storyteller_f.giant_explorer.FileViewHolder
import com.storyteller_f.giant_explorer.databinding.DialogRequestPathBinding
import com.storyteller_f.giant_explorer.service
import com.storyteller_f.giant_explorer.supportDirectoryContent
import com.storyteller_f.ui_list.core.SearchProducer
import com.storyteller_f.ui_list.core.SimpleSourceAdapter
import com.storyteller_f.ui_list.core.search
import kotlinx.parcelize.Parcelize

class RequestPathDialog :
    CommonDialogFragment<DialogRequestPathBinding>(DialogRequestPathBinding::inflate) {
    private val fileInstance by vm {
        GenericValueModel<FileInstance>().apply {
            data.value =
                FileInstanceFactory.getFileInstance("/storage/emulated/0", requireContext())
        }
    }

    @Parcelize
    class RequestPathResult(val path: String) : Parcelable

    private val data by search(
        SearchProducer(::service) { it, _ ->
            FileItemHolder(it, MutableLiveData(mutableListOf()))
        }
    )
    private val adapter = SimpleSourceAdapter<FileItemHolder, FileViewHolder>()

    override fun onBindViewEvent(binding: DialogRequestPathBinding) {
        binding.bottom.requestScreenOn.setOnClick {
            it.keepScreenOn = it.isChecked
        }
        binding.bottom.positive.setOnClick {
            fileInstance.data.value?.path?.let {
                setFragmentResult("request-path", RequestPathResult(it))
                dismiss()
            }
        }
        binding.bottom.negative.setOnClick {
            dismiss()
        }
        supportDirectoryContent(
            binding.content,
            binding.pathMan,
            adapter,
            fileInstance,
            data,
            null
        )
    }

    @BindClickEvent(FileItemHolder::class)
    fun toChild(itemHolder: FileItemHolder) {
        if (itemHolder.file.item.isDirectory) {
            val fileInstance1 = fileInstance.data.value ?: return
            fileInstance.data.value = FileInstanceFactory.toChild(
                fileInstance1,
                itemHolder.file.name,
                false,
                requireContext(),
                false
            )
        }
    }
}