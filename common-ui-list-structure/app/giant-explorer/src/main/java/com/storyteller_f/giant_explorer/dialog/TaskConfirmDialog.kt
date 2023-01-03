package com.storyteller_f.giant_explorer.dialog

import android.os.Parcelable
import android.widget.LinearLayout
import android.widget.TextView
import com.storyteller_f.common_ui.SimpleDialogFragment
import com.storyteller_f.common_ui.setFragmentResult
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.common_vm_ktx.keyPrefix
import com.storyteller_f.common_vm_ktx.pvm
import com.storyteller_f.giant_explorer.control.TempVM
import com.storyteller_f.giant_explorer.databinding.DialogTaskConfirmBinding
import com.storyteller_f.giant_explorer.databinding.ViewHolderTaskConfirmItemBinding
import kotlinx.parcelize.Parcelize
import java.io.File

class TaskConfirmDialog : SimpleDialogFragment<DialogTaskConfirmBinding>(DialogTaskConfirmBinding::inflate) {
    private val tempVM by keyPrefix({ "temp" }, pvm({}) {
        TempVM()
    })

    override fun onBindViewEvent(binding: DialogTaskConfirmBinding) {
        binding.labelDest.text = tempVM.dest
        tempVM.list.forEach {
            val file = File(it)
            val inflate = ViewHolderTaskConfirmItemBinding.inflate(layoutInflater, binding.selected, true)
            inflate.name.text = file.name
            inflate.path.text = it
        }
        binding.include.negative.setOnClick {
            dismiss()
        }
        binding.include.positive.setOnClick {
            setFragmentResult(Result(true))
            dismiss()
        }
    }

    @Parcelize
    class Result(val confirm: Boolean) : Parcelable
}