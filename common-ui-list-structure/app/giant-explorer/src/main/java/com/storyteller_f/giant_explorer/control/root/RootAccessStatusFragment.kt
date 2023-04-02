package com.storyteller_f.giant_explorer.control.root

import androidx.navigation.fragment.findNavController
import com.storyteller_f.common_ui.SimpleFragment
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.databinding.FragmentRootAccessStatusBinding
import com.topjohnwu.superuser.Shell

class RootAccessStatusFragment : SimpleFragment<FragmentRootAccessStatusBinding>(FragmentRootAccessStatusBinding::inflate) {

    override fun onBindViewEvent(binding: FragmentRootAccessStatusBinding) {
        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_RootAccessStatusFragment_to_Second2Fragment)
        }
        val status = binding.status
        Shell.getShell {
            status.text = it.isRoot.toString()
        }
    }
}