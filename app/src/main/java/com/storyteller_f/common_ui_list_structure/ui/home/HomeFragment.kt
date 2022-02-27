package com.storyteller_f.common_ui_list_structure.ui.home

import android.widget.TextView
import androidx.fragment.app.viewModels
import com.storyteller_f.common_ui.CommonFragment
import com.storyteller_f.common_ui_list_structure.databinding.FragmentHomeBinding

class HomeFragment : CommonFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val homeViewModel by viewModels<HomeViewModel>()

    override fun onBindViewEvent(binding: FragmentHomeBinding) {
        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
    }

}