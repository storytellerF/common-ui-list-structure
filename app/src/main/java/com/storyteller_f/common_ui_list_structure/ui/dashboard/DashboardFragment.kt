package com.storyteller_f.common_ui_list_structure.ui.dashboard

import android.view.View
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.storyteller_f.common_ui.CommonFragment
import com.storyteller_f.common_ui_list_structure.R
import com.storyteller_f.common_ui_list_structure.databinding.FragmentDashboardBinding
import com.storyteller_f.ui_list.event.viewBinding

class DashboardFragment : CommonFragment(R.layout.fragment_dashboard) {

    private val binding: FragmentDashboardBinding by viewBinding(FragmentDashboardBinding::bind)
    private val dashboardViewModel by viewModels<DashboardViewModel>()

    override fun onBindViewEvent(inflate: View) {
        val textView: TextView = binding.textDashboard
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
    }
}