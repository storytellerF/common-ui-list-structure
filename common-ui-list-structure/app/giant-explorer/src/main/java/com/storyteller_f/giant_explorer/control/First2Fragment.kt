package com.storyteller_f.giant_explorer.control

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.storyteller_f.common_ui.SimpleFragment
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.databinding.FragmentFirst2Binding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class First2Fragment : SimpleFragment<FragmentFirst2Binding>(FragmentFirst2Binding::inflate) {

    override fun onBindViewEvent(binding: FragmentFirst2Binding) {
        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_First2Fragment_to_Second2Fragment)
        }
    }
}