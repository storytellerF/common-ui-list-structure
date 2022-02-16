package com.storyteller_f.common_ui_list_structure.ui.notifications

import android.widget.TextView
import androidx.fragment.app.viewModels
import com.storyteller_f.common_ui.CommonFragment
import com.storyteller_f.common_ui_list_structure.databinding.FragmentNotificationsBinding

class NotificationsFragment : CommonFragment<FragmentNotificationsBinding>(FragmentNotificationsBinding::inflate) {

    private val notificationsViewModel by viewModels<NotificationsViewModel>()

    override fun onBindViewEvent(binding: FragmentNotificationsBinding) {
        val textView: TextView = binding.textNotifications
        notificationsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
    }
}