package com.storyteller_f.ping

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.flowWithLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigatorExtras
import com.bumptech.glide.Glide
import com.storyteller_f.annotation_defination.BindClickEvent
import com.storyteller_f.annotation_defination.BindItemHolder
import com.storyteller_f.annotation_defination.BindLongClickEvent
import com.storyteller_f.common_ui.SimpleFragment
import com.storyteller_f.common_ui.cycle
import com.storyteller_f.common_ui.scope
import com.storyteller_f.ping.database.Wallpaper
import com.storyteller_f.ping.database.requireMainDatabase
import com.storyteller_f.ping.databinding.FragmentWallpaperListBinding
import com.storyteller_f.ping.databinding.ViewHolderWallpaperBinding
import com.storyteller_f.ui_list.adapter.ManualAdapter
import com.storyteller_f.ui_list.core.AbstractViewHolder
import com.storyteller_f.ui_list.core.BindingViewHolder
import com.storyteller_f.ui_list.core.DataItemHolder
import com.storyteller_f.ui_list.ui.ListWithState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WallpaperListFragment : SimpleFragment<FragmentWallpaperListBinding>(FragmentWallpaperListBinding::inflate) {
    private val adapter = ManualAdapter<DataItemHolder, AbstractViewHolder<DataItemHolder>>()

    override fun onBindViewEvent(binding: FragmentWallpaperListBinding) {

    }

    private val setWallpaper = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.i(TAG, "choose: result ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                val first = requireContext().dataStore.data.mapNotNull {
                    it[preview]
                }.first()
                requireContext().dataStore.edit {
                    it[selected] = first
                    it[preview] = ""
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.manualUp(adapter)
        binding.content.flash(ListWithState.UIState.loading)
        scope.launch {
            requireMainDatabase.dao().selectAll().flowWithLifecycle(cycle).shareIn(scope, SharingStarted.WhileSubscribed()).collectLatest { wallpaperList ->
                binding.content.flash(ListWithState.UIState(false, wallpaperList.isNotEmpty(), empty = false, progress = false, null, null))
                val map = wallpaperList.map {
                    WallpaperHolder(it)
                }
                adapter.submitList(map)
            }
        }
    }

    @BindClickEvent(WallpaperHolder::class)
    fun clickWallpaper(itemHolder: WallpaperHolder) {
        scope.launch {
            requireContext().dataStore.edit {
                it[preview] = itemHolder.wallpaper.uri
            }
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(requireActivity(), PingPagerService::class.java)
            )
            setWallpaper.launch(intent)
        }

    }

    @BindLongClickEvent(WallpaperHolder::class)
    fun longClickWallpaper(holderView: View, itemHolder: WallpaperHolder, binding: ViewHolderWallpaperBinding) {
        holderView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        val fragmentNavigatorExtras = FragmentNavigatorExtras(binding.wallpaperPreview to "wallpaper-preview")
        holderView.findNavController()
            .navigate(R.id.action_WallpaperListFragment_to_WallpaperInfoFragment, WallpaperInfoFragmentArgs(itemHolder.wallpaper.uri).toBundle(), null, navigatorExtras = fragmentNavigatorExtras)
    }

    companion object {
        private const val TAG = "WallpaperListFragment"
    }
}

class WallpaperHolder(val wallpaper: Wallpaper) : DataItemHolder {
    override fun areItemsTheSame(other: DataItemHolder): Boolean {
        return (other as WallpaperHolder).wallpaper == wallpaper
    }
}

@BindItemHolder(WallpaperHolder::class)
class WallpaperViewHolder(private val binding: ViewHolderWallpaperBinding) : BindingViewHolder<WallpaperHolder>(binding) {
    override fun bindData(itemHolder: WallpaperHolder) {
        binding.flash(itemHolder.wallpaper)
        ViewCompat.setTransitionName(binding.wallpaperPreview, "wallpaper-${itemHolder.wallpaper.uri}")
    }

}

fun ViewHolderWallpaperBinding.flash(wallpaper: Wallpaper) {
    wallpaperUri.text = wallpaper.uri
    wallpaperName.text = wallpaper.name
    Glide.with(wallpaperPreview).load(wallpaper.thumbnail).into(wallpaperPreview)
}