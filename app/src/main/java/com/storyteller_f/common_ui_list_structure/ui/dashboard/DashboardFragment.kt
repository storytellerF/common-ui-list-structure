package com.storyteller_f.common_ui_list_structure.ui.dashboard

import android.widget.TextView
import androidx.fragment.app.viewModels
import com.storyteller_f.common_ui.CommonFragment
import com.storyteller_f.common_ui_list_structure.databinding.FragmentDashboardBinding

class DashboardFragment : CommonFragment<FragmentDashboardBinding>(FragmentDashboardBinding::inflate) {

    private val dashboardViewModel by viewModels<DashboardViewModel>()

    override fun onBindViewEvent(binding: FragmentDashboardBinding) {
        val textView: TextView = binding.textDashboard
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
    }
}