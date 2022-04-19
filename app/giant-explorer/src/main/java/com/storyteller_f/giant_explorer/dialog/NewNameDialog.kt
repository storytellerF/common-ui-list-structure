package com.storyteller_f.giant_explorer.dialog

import android.os.Bundle
import androidx.fragment.app.setFragmentResult
import com.storyteller_f.common_ui.CommonDialogFragment
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.giant_explorer.databinding.DialogNewNameBinding

class NewNameDialog:CommonDialogFragment<DialogNewNameBinding>(DialogNewNameBinding::inflate) {
    override fun onBindViewEvent(binding: DialogNewNameBinding) {
        binding.bottom.positive.setOnClick {
            setFragmentResult("add-file", Bundle().apply {
                putString("name", binding.editTextTextPersonName.text.toString())
            })
            dismiss()
        }
        binding.bottom.negative.setOnClick {
            dismiss()
        }
    }
}