package com.storyteller_f.giant_explorer.control

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.navArgs
import com.storyteller_f.common_ui.SimpleFragment
import com.storyteller_f.common_ui.scope
import com.storyteller_f.giant_explorer.FragmentPluginConfiguration
import com.storyteller_f.giant_explorer.HtmlPluginConfiguration
import com.storyteller_f.giant_explorer.databinding.FragmentPluginInfoBinding
import com.storyteller_f.giant_explorer.pluginManagerRegister

class PluginInfoFragment : SimpleFragment<FragmentPluginInfoBinding>(FragmentPluginInfoBinding::inflate) {
    private val args by navArgs<PluginInfoFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scope.launchWhenResumed {
            val pluginConfiguration = pluginManagerRegister.resolvePluginName(args.pluginName, requireContext())
            binding.pluginName.text = args.pluginName
            binding.pluginPath.text = pluginConfiguration.meta.path
            when (pluginConfiguration) {
                is FragmentPluginConfiguration -> {
                    binding.other.text = pluginConfiguration.startFragment
                }
                is HtmlPluginConfiguration -> {
                    binding.other.text = pluginConfiguration.extractedPath
                }
            }
        }
    }

    override fun onBindViewEvent(binding: FragmentPluginInfoBinding) {

    }

}