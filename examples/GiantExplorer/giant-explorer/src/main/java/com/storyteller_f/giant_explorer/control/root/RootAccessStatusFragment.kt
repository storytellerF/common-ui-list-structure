package com.storyteller_f.giant_explorer.control.root

import android.app.Activity
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.navigation.fragment.findNavController
import com.storyteller_f.common_ui.SimpleFragment
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.common_vm_ktx.StateValueModel
import com.storyteller_f.common_vm_ktx.svm
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.databinding.FragmentRootAccessStatusBinding
import com.topjohnwu.superuser.Shell

class RootAccessStatusFragment : SimpleFragment<FragmentRootAccessStatusBinding>(FragmentRootAccessStatusBinding::inflate) {

    private val state by svm({}) { handle, _ ->
        StateValueModel(handle, default = false)
    }

    override fun onBindViewEvent(binding: FragmentRootAccessStatusBinding) {
        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_RootAccessStatusFragment_to_Second2Fragment)
        }
        Shell.getShell {
            state.data.value = it.isRoot
        }
        binding.status.setOnClick {
            val uri = Uri.Builder().scheme(ContentResolver.SCHEME_FILE).path("/").build()
            activity?.let {
                it.setResult(Activity.RESULT_OK, Intent().setData(uri))
                it.finish()
            }
        }
        state.data.observe(viewLifecycleOwner) {
            binding.status.isEnabled = it
        }
    }
}