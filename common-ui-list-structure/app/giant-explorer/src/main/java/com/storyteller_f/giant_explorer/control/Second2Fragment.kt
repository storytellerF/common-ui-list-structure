package com.storyteller_f.giant_explorer.control

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.storyteller_f.common_ui.SimpleFragment
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.databinding.FragmentSecond2Binding

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class Second2Fragment : SimpleFragment<FragmentSecond2Binding>(FragmentSecond2Binding::inflate) {

    override fun onBindViewEvent(binding: FragmentSecond2Binding) {
        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_Second2Fragment_to_First2Fragment)
        }
    }

}