package com.storyteller_f.common_ui_list_structure

import android.os.Parcelable
import com.storyteller_f.common_ui.CommonDialogFragment
import com.storyteller_f.common_ui.setFragmentResult
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.common_ui_list_structure.databinding.DialogTestBinding
import kotlinx.parcelize.Parcelize

class TestDialog : CommonDialogFragment<DialogTestBinding>(DialogTestBinding::inflate) {
    override fun onBindViewEvent(binding: DialogTestBinding) {
        binding.button.setOnClick {
            setFragmentResult(Result("ok"))
            dismiss()
        }
    }

    @Parcelize
    class Result(val test: String) : Parcelable
}

class TestDialog2 : CommonDialogFragment<DialogTestBinding>(DialogTestBinding::inflate) {
    override fun onBindViewEvent(binding: DialogTestBinding) {
        binding.button.setOnClick {
            setFragmentResult(Result("--------"))
            dismiss()
        }
    }

    @Parcelize
    class Result(val test: String) : Parcelable
}