package com.storyteller_f.common_ui_list_structure

import android.os.Parcelable
import com.storyteller_f.common_ui.CommonDialogFragment
import com.storyteller_f.common_ui.SimpleDialogFragment
import com.storyteller_f.common_ui.setFragmentResult
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.common_ui_list_structure.databinding.DialogTestBinding
import kotlinx.parcelize.Parcelize

class TestDialog : SimpleDialogFragment<DialogTestBinding>(DialogTestBinding::inflate) {
    override fun onBindViewEvent(binding: DialogTestBinding) {
        binding.button.setOnClick {
            setFragmentResult(Result("Test Dialog"))
            dismiss()
        }
    }

    @Parcelize
    class Result(val test: String) : Parcelable
}

class TestDialog2 : SimpleDialogFragment<DialogTestBinding>(DialogTestBinding::inflate) {
    override fun onBindViewEvent(binding: DialogTestBinding) {
        binding.button.setOnClick {
            setFragmentResult(Result("TestDialog 2"))
            dismiss()
        }
    }

    @Parcelize
    class Result(val test: String) : Parcelable
}