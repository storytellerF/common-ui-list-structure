package com.storyteller_f.giant_explorer.dialog

import android.os.Parcelable
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.common_ui.CommonDialogFragment
import com.storyteller_f.common_ui.setFragmentResult
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system_ktx.isDirectory
import com.storyteller_f.giant_explorer.control.*
import com.storyteller_f.giant_explorer.databinding.DialogRequestPathBinding
import com.storyteller_f.ui_list.core.SearchProducer
import com.storyteller_f.ui_list.core.SimpleSourceAdapter
import com.storyteller_f.ui_list.core.search
import kotlinx.parcelize.Parcelize

class RequestPathDialog :
    CommonDialogFragment<DialogRequestPathBinding>(DialogRequestPathBinding::inflate) {
    private val session by viewModels<FileExplorerSession>()

    @Parcelize
    class RequestPathResult(val path: String) : Parcelable

    private val data by search(
        SearchProducer(::service) { it, _ ->
            FileItemHolder(it, MutableLiveData(mutableListOf()))
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
        binding.bottom.negative.setOnClick {
            dismiss()
        }
        supportDirectoryContent(binding.content, binding.pathMan, adapter, data, session)
        session.init(this)
    }

    @BindClickEvent(FileItemHolder::class)
    fun toChild(itemHolder: FileItemHolder) {
        if (itemHolder.file.item.isDirectory) {
            val fileInstance1 = session.fileInstance.value ?: return
            session.fileInstance.value = FileInstanceFactory.toChild(
                fileInstance1,
                itemHolder.file.name,
                false,
                requireContext(),
                false
            )
        }
    }
}