package com.storyteller_f.giant_explorer.dialog

import android.os.Parcelable
import com.storyteller_f.common_ui.CommonDialogFragment
import com.storyteller_f.common_ui.setFragmentResult
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.giant_explorer.databinding.DialogNewNameBinding
import kotlinx.parcelize.Parcelize

class NewNameDialog : CommonDialogFragment<DialogNewNameBinding>(DialogNewNameBinding::inflate) {
    override fun onBindViewEvent(binding: DialogNewNameBinding) {
        binding.bottom.positive.setOnClick {
            setFragmentResult(NewNameResult(binding.newName.text.toString()))
            dismiss()
        }
        binding.bottom.negative.setOnClick {
            dismiss()
        }
    }

    @Parcelize
    class NewNameResult(val name: String) : Parcelable

    companion object{
        const val requestKey = "add-file"
    }

    override fun requestKey(): String {
        return requestKey
    }
}