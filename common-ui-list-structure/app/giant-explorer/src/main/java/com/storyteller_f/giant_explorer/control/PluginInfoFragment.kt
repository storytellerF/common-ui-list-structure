package com.storyteller_f.giant_explorer.control

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.storyteller_f.common_ui.SimpleFragment
import com.storyteller_f.common_ui.cycle
import com.storyteller_f.common_ui.scope
import com.storyteller_f.giant_explorer.FragmentPluginConfiguration
import com.storyteller_f.giant_explorer.HtmlPluginConfiguration
import com.storyteller_f.giant_explorer.databinding.FragmentPluginInfoBinding
import com.storyteller_f.giant_explorer.pluginManagerRegister
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PluginInfoFragment : SimpleFragment<FragmentPluginInfoBinding>(FragmentPluginInfoBinding::inflate) {
    private val args by navArgs<PluginInfoFragmentArgs>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val pluginConfiguration = withContext(Dispatchers.IO) {
                    pluginManagerRegister.resolvePluginName(args.pluginName, requireContext())
                }
                binding.pluginName.text = "${args.pluginName} - ${pluginConfiguration.meta.version}"
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
    }

    override fun onBindViewEvent(binding: FragmentPluginInfoBinding) {

    }

}