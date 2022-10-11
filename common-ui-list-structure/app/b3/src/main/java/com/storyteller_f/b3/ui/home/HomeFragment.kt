package com.storyteller_f.b3.ui.home

import android.content.Intent
import com.storyteller_f.b3.databinding.FragmentHomeBinding
import com.storyteller_f.b3.ui.login.LoginActivity
import com.storyteller_f.common_ui.CommonFragment
import com.storyteller_f.common_ui.SimpleFragment

class HomeFragment : SimpleFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    override fun onBindViewEvent(binding: FragmentHomeBinding) {
        binding.textHome.setOnClickListener {
            //进入登录流程
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
    }
}