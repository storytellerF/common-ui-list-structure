package com.storyteller_f.giant_explorer.dialog

import android.os.Build
import androidx.core.view.isVisible
import com.storyteller_f.common_ui.SimpleDialogFragment
import com.storyteller_f.file_system.util.FileUtility
import com.storyteller_f.giant_explorer.control.format1024
import com.storyteller_f.giant_explorer.databinding.DialogVolumeSpaceBinding
import com.storyteller_f.giant_explorer.databinding.LayoutVolumeItemBinding

class VolumeSpaceDialog: SimpleDialogFragment<DialogVolumeSpaceBinding>(DialogVolumeSpaceBinding::inflate) {
    override fun onBindViewEvent(binding: DialogVolumeSpaceBinding) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileUtility.getStorageVolume(requireContext()).forEach {
                LayoutVolumeItemBinding.inflate(layoutInflater, binding.spaceList, true).apply {
                    val prefix = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        it.directory?.absolutePath
                    } else {
                        FileUtility.volumePathName(it.uuid)
                    }
                    volumeSpace.text = format1024(FileUtility.getSpace(prefix))
                    volumeFree.text = format1024(FileUtility.getFree(prefix))
                    volumeTotal.text = format1024(FileUtility.getTotal(prefix))
                    listOf(volumeSpace, volumeTotal, volumeFree).forEach {
                        it.copyTextFeature()
                    }
                    volumeName.text = it.getDescription(requireContext())
                    info.isVisible = true
                    volumeState.text = it.state
                }
            }
        } else {
            FileUtility.getStorageCompat(requireContext()).forEach {
                LayoutVolumeItemBinding.inflate(layoutInflater, binding.spaceList, true).apply {
                    volumeSpace.text = format1024(FileUtility.getSpace(it.absolutePath))
                    volumeName.text = it.name
                    info.isVisible = false
                }
            }
        }
    }
}