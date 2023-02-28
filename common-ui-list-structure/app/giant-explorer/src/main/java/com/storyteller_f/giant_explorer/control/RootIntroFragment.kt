package com.storyteller_f.giant_explorer.control

import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.storyteller_f.common_ui.SimpleFragment
import com.storyteller_f.giant_explorer.databinding.FragmentRootIntroBinding

class RootIntroFragment : SimpleFragment<FragmentRootIntroBinding>(FragmentRootIntroBinding::inflate) {

    override fun onBindViewEvent(binding: FragmentRootIntroBinding) {
        binding.button.setOnClickListener {
            val url = "https://github.com/topjohnwu/Magisk"
            val builder = CustomTabsIntent.Builder()
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(requireContext(), Uri.parse(url))
        }
    }

}