package com.storyteller_f.common_ui_list_structure.ui.notifications

import android.view.View
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.storyteller_f.common_ui.CommonFragment
import com.storyteller_f.common_ui_list_structure.R
import com.storyteller_f.common_ui_list_structure.databinding.FragmentNotificationsBinding
import com.storyteller_f.ui_list.event.viewBinding

class NotificationsFragment : CommonFragment(R.layout.fragment_notifications) {

    private val binding by viewBinding(FragmentNotificationsBinding::bind)
    private val notificationsViewModel by viewModels<NotificationsViewModel>()

    override fun onBindViewEvent(inflate: View) {
        val textView: TextView = binding.textNotifications
        notificationsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
    }
}