package com.storyteller_f.common_ui_list_structure.test_model

import android.widget.TextView
import com.storyteller_f.common_ui.RegularFragment
import com.storyteller_f.common_ui_list_structure.api.requireReposService
import com.storyteller_f.common_ui_list_structure.databinding.FragmentTestDetailBinding
import com.storyteller_f.common_ui_list_structure.db.requireRepoDatabase
import com.storyteller_f.ui_list.source.DetailProducer
import com.storyteller_f.ui_list.source.detail

class TestDetailViewModelFragment : RegularFragment<FragmentTestDetailBinding>(FragmentTestDetailBinding::inflate) {

    private val detail by detail(DetailProducer(
        {
            requireReposService.searchRepos(1, 1).items.first()
        }, {
            requireRepoDatabase.reposDao().select()
        }
    ))

    override fun onBindViewEvent(binding: FragmentTestDetailBinding) {
        val textView: TextView = binding.textNotifications
        detail.content.observe(viewLifecycleOwner) {
            textView.text = it.fullName
        }
    }

}