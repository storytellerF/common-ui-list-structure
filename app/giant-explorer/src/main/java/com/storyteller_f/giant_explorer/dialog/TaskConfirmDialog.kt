package com.storyteller_f.giant_explorer.dialog

import android.os.Parcelable
import com.storyteller_f.common_ui.SimpleDialogFragment
import com.storyteller_f.common_ui.setFragmentResult
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.common_vm_ktx.keyPrefix
import com.storyteller_f.common_vm_ktx.pvm
import com.storyteller_f.giant_explorer.control.TempVM
import com.storyteller_f.giant_explorer.databinding.DialogTaskConfirmBinding
import kotlinx.parcelize.Parcelize

class TaskConfirmDialog : SimpleDialogFragment<DialogTaskConfirmBinding>(DialogTaskConfirmBinding::inflate) {
    private val temp by keyPrefix({ "temp" }, pvm({}) {
        TempVM()
    })

    override fun onBindViewEvent(binding: DialogTaskConfirmBinding) {
        binding.labelDest.text = "dest:${temp.dest}\nlist:${temp.list.joinToString("\n")}"
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