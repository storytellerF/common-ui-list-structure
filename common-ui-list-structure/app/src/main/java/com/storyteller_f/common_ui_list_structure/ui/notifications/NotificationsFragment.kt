package com.storyteller_f.common_ui_list_structure.ui.notifications

import android.widget.TextView
import com.storyteller_f.common_ui.SimpleFragment
import com.storyteller_f.common_ui_list_structure.api.requireReposService
import com.storyteller_f.common_ui_list_structure.databinding.FragmentNotificationsBinding
import com.storyteller_f.common_ui_list_structure.db.requireRepoDatabase
import com.storyteller_f.ui_list.source.DetailProducer
import com.storyteller_f.ui_list.source.detail

class NotificationsFragment : SimpleFragment<FragmentNotificationsBinding>(FragmentNotificationsBinding::inflate) {

    private val detail by detail(DetailProducer(
        {
            requireReposService().searchRepos(1, 1).items.first()
        }, {
            requireRepoDatabase.reposDao().select()
        }
    ))
    override fun onBindViewEvent(binding: FragmentNotificationsBinding) {
        val textView: TextView = binding.textNotifications
        detail.content.observe(viewLifecycleOwner) {
            textView.text = it.fullName
        }
    }

    override fun requestKey(): String {
        return "notification"
    }
}