package com.storyteller_f.giant_explorer.control

import android.os.Bundle
import android.view.View
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.common_ui.SimpleFragment
import com.storyteller_f.common_ui.scope
import com.storyteller_f.giant_explorer.PluginManager
import com.storyteller_f.giant_explorer.databinding.FragmentFirstBinding
import com.storyteller_f.giant_explorer.databinding.ViewHolderPluginBinding
import com.storyteller_f.ui_list.adapter.ManualAdapter
import com.storyteller_f.ui_list.core.AbstractViewHolder
import com.storyteller_f.ui_list.core.AdapterViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.ui.ListWithState
import kotlinx.coroutines.launch

class PluginListFragment : SimpleFragment<FragmentFirstBinding>(FragmentFirstBinding::inflate) {

    private val adapter = ManualAdapter<DataItemHolder, AbstractViewHolder<DataItemHolder>>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.manualUp(adapter)
        binding.content.flash(ListWithState.UIState.loading)
        scope.launch {

            val map = PluginManager.list.map {
                PluginHolder(it.name)
            }
            binding.content.flash(ListWithState.UIState(false, map.isNotEmpty(), empty = false, progress = false, null, null))
            adapter.submitList(map)
        }
    }

    override fun onBindViewEvent(binding: FragmentFirstBinding) {

    }

}

class PluginHolder(val name: String) : DataItemHolder {
    override fun areItemsTheSame(other: DataItemHolder): Boolean {
        return (other as PluginHolder).name == name
    }

}

@BindItemHolder(PluginHolder::class)
class PluginViewHolder(private val binding: ViewHolderPluginBinding) : AdapterViewHolder<PluginHolder>(binding) {
    override fun bindData(itemHolder: PluginHolder) {
        binding.pluginName.text = itemHolder.name
    }

}