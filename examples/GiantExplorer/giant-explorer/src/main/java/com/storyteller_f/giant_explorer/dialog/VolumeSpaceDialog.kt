package com.storyteller_f.giant_explorer.dialog

import android.os.Build
import androidx.core.view.isVisible
import com.storyteller_f.common_ui.SimpleDialogFragment
import com.storyteller_f.file_system.util.getFree
import com.storyteller_f.file_system.util.getSpace
import com.storyteller_f.file_system.util.getStorageCompat
import com.storyteller_f.file_system.util.getStorageVolume
import com.storyteller_f.file_system.util.getTotal
import com.storyteller_f.file_system.util.volumePathName
import com.storyteller_f.giant_explorer.control.format1024
import com.storyteller_f.giant_explorer.databinding.DialogVolumeSpaceBinding
import com.storyteller_f.giant_explorer.databinding.LayoutVolumeItemBinding

class VolumeSpaceDialog :
    SimpleDialogFragment<DialogVolumeSpaceBinding>(DialogVolumeSpaceBinding::inflate) {
    override fun onBindViewEvent(binding: DialogVolumeSpaceBinding) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requireContext().getStorageVolume().forEach {
                LayoutVolumeItemBinding.inflate(layoutInflater, binding.spaceList, true).apply {
                    val prefix = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        it.directory?.absolutePath
                    } else {
                        volumePathName(it.uuid)
                    }
                    volumeSpace.text = format1024(getSpace(prefix))
                    volumeFree.text = format1024(getFree(prefix))
                    volumeTotal.text = format1024(getTotal(prefix))
                    listOf(volumeSpace, volumeTotal, volumeFree).forEach {
                        it.copyTextFeature()
                    }
                    volumeName.text = it.getDescription(requireContext())
                    info.isVisible = true
                    volumeState.text = it.state
                }
            }
        } else {
            requireContext().getStorageCompat().forEach {
                LayoutVolumeItemBinding.inflate(layoutInflater, binding.spaceList, true).apply {
                    volumeSpace.text = format1024(getSpace(it.absolutePath))
                    volumeName.text = it.name
                    info.isVisible = false
                }
            }
        }
    }
}