package com.storyteller_f.giant_explorer.control

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.common_ui.SimpleFragment
import com.storyteller_f.common_ui.owner
import com.storyteller_f.common_ui.scope
import com.storyteller_f.giant_explorer.*
import com.storyteller_f.giant_explorer.databinding.FragmentPluginListBinding
import com.storyteller_f.giant_explorer.databinding.ViewHolderPluginBinding
import com.storyteller_f.ui_list.adapter.ManualAdapter
import com.storyteller_f.ui_list.adapter.SimpleSourceAdapter
import com.storyteller_f.ui_list.core.AbstractViewHolder
import com.storyteller_f.ui_list.core.AdapterViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.core.Model
import com.storyteller_f.ui_list.data.SimpleResponse
import com.storyteller_f.ui_list.source.*
import com.storyteller_f.ui_list.ui.ListWithState
import kotlinx.coroutines.launch
import java.io.File

class SimplePlugin(val path: String) : Model {
    override fun commonId(): String {
        return path
    }
}

class PluginListFragment : SimpleFragment<FragmentPluginListBinding>(FragmentPluginListBinding::inflate) {

    private val adapter = SimpleSourceAdapter<PluginHolder, PluginViewHolder>()
    private val source by search(SearchProducer<SimplePlugin, String, PluginHolder>(service = { _, startPage, count ->
        val toList = pluginManagerRegister.pluginsName().toList()
        val startIndex = (startPage - 1) * count
        val toIndex = (startIndex + count).coerceAtMost(toList.size)
        val data = toList.subList(startIndex, toIndex).map {
            SimplePlugin(it)
        }
        SimpleResponse(toList.size, data)
    }, processFactory = { p, _ ->
        PluginHolder(p.path)
    }))

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.sourceUp(adapter, owner, refresh = {
            pluginManagerRegister.removeAllPlugin()
            refreshPlugin(requireContext())
        }, flash = ListWithState.Companion::remote)
        source.observerInScope(this, "") {
            adapter.submitData(it)
        }

    }

    override fun onBindViewEvent(binding: FragmentPluginListBinding) {

    }

    @BindClickEvent(PluginHolder::class)
    fun clickPlugin(itemHolder: PluginHolder) {
        findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment, PluginInfoFragmentArgs(itemHolder.name).toBundle())
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