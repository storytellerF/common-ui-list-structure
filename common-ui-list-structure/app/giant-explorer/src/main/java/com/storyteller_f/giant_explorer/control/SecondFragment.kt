package com.storyteller_f.giant_explorer.control

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.storyteller_f.common_ui.SimpleFragment
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.databinding.FragmentSecondBinding

class SecondFragment : SimpleFragment<FragmentSecondBinding>(FragmentSecondBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
        }
    }

    override fun onBindViewEvent(binding: FragmentSecondBinding) {
        TODO("Not yet implemented")
    }

}