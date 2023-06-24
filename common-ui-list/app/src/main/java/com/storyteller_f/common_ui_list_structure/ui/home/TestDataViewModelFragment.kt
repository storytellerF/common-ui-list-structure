package com.storyteller_f.common_ui_list_structure.ui.home

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.storyteller_f.common_ui.RegularFragment
import com.storyteller_f.common_ui.scope
import com.storyteller_f.common_ui_list_structure.Repo2ViewHolder
import com.storyteller_f.common_ui_list_structure.RepoItemHolder
import com.storyteller_f.common_ui_list_structure.api.requireReposService
import com.storyteller_f.common_ui_list_structure.databinding.FragmentTestDataBinding
import com.storyteller_f.ui_list.adapter.SimpleDataAdapter
import com.storyteller_f.ui_list.source.DataProducer
import com.storyteller_f.ui_list.source.data
import kotlinx.coroutines.launch

class TestDataViewModelFragment : RegularFragment<FragmentTestDataBinding>(FragmentTestDataBinding::inflate) {

    private val data by data(
        DataProducer(
            { p, size ->
                requireReposService.searchRepos(p, size)
            },
            { it, _ -> RepoItemHolder(it) },
        )
    )
    private val adapter = SimpleDataAdapter<RepoItemHolder, Repo2ViewHolder>()

    override fun onBindViewEvent(binding: FragmentTestDataBinding) {
        binding.listWithState.dataUp(adapter, viewLifecycleOwner, data)
        scope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                data.content.observe(viewLifecycleOwner) {
                    adapter.submitData(it)
                }
            }
        }
    }

}