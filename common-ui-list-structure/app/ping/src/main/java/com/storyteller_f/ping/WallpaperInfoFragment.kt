package com.storyteller_f.ping

import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionInflater
import com.storyteller_f.common_ui.SimpleFragment
import com.storyteller_f.common_ui.cycle
import com.storyteller_f.common_ui.scope
import com.storyteller_f.ping.database.requireMainDatabase
import com.storyteller_f.ping.databinding.FragmentWallpaperInfoBinding
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class WallpaperInfoFragment : SimpleFragment<FragmentWallpaperInfoBinding>(FragmentWallpaperInfoBinding::inflate) {
    val args: WallpaperInfoFragmentArgs by navArgs()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(requireContext())
            .inflateTransition(R.transition.shared_image)

    }
    override fun onBindViewEvent(binding: FragmentWallpaperInfoBinding) {
        ViewCompat.setTransitionName(binding.wallpaperCard.wallpaperPreview, "wallpaper-preview")
        scope.launch {
            requireMainDatabase.dao().select(args.uri).flowWithLifecycle(cycle).shareIn(scope, SharingStarted.WhileSubscribed()).collectLatest {
                binding.wallpaperCard.flash(it)
                binding.created.text = it.createdTime.toString()
            }
        }
    }
}