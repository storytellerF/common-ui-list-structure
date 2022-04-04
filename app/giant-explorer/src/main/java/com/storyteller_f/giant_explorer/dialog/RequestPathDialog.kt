package com.storyteller_f.giant_explorer.dialog

import androidx.lifecycle.MutableLiveData
import com.storyteller_f.common_ui.CommonDialogFragment
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.common_vm_ktx.GenericValueModel
import com.storyteller_f.common_vm_ktx.vm
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.giant_explorer.FileItemHolder
import com.storyteller_f.giant_explorer.FileViewHolder
import com.storyteller_f.giant_explorer.databinding.DialogRequestPathBinding
import com.storyteller_f.giant_explorer.service
import com.storyteller_f.giant_explorer.supportDirectoryContent
import com.storyteller_f.ui_list.core.SearchProducer
import com.storyteller_f.ui_list.core.SimpleSourceAdapter
import com.storyteller_f.ui_list.core.search

class RequestPathDialog :
    CommonDialogFragment<DialogRequestPathBinding>(DialogRequestPathBinding::inflate) {
    private val fileInstance by vm {
        GenericValueModel<FileInstance>().apply {
            data.value =
                FileInstanceFactory.getFileInstance("/storage/emulated/0", requireContext())
        }
    }

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
            callback?.onOk(fileInstance.data.value!!)
            dismiss()
        }
        binding.bottom.negative.setOnClick {
            callback?.onCancel()
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

    var callback: Callback? = null

    interface Callback {
        fun onOk(fileInstance: FileInstance)

        fun onCancel()
    }
}