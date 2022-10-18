package com.storyteller_f.common_ui_list_structure.ui.home

import androidx.lifecycle.lifecycleScope
import com.storyteller_f.common_ui.RegularFragment
import com.storyteller_f.common_ui_list_structure.Repo2ViewHolder
import com.storyteller_f.common_ui_list_structure.RepoItemHolder
import com.storyteller_f.common_ui_list_structure.api.requireReposService
import com.storyteller_f.common_ui_list_structure.databinding.FragmentHomeBinding
import com.storyteller_f.ui_list.adapter.SimpleDataAdapter
import com.storyteller_f.ui_list.source.DataProducer
import com.storyteller_f.ui_list.source.data

class HomeFragment : RegularFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val data by data(
        DataProducer(
            { p, size ->
                requireReposService().searchRepos(p, size)
            },
            { it, _ -> RepoItemHolder(it) },
        )
    )
    private val adapter = SimpleDataAdapter<RepoItemHolder, Repo2ViewHolder>()

    override fun onBindViewEvent(binding: FragmentHomeBinding) {
        binding.listWithState.dataUp(adapter, viewLifecycleOwner, data)
        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            data.content.observe(viewLifecycleOwner) {
                adapter.submitData(it)
            }
        }
    }

    override fun requestKey(): String {
        return "home"
    }

}