package com.storyteller_f.giant_explorer.dialog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.navArgs
import com.storyteller_f.common_ui.SimpleDialogFragment
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.giant_explorer.control.getFileInstance
import com.storyteller_f.giant_explorer.databinding.DialogFilePropertiesBinding
import com.storyteller_f.giant_explorer.model.FileModel

class PropertiesDialog : SimpleDialogFragment<DialogFilePropertiesBinding>(DialogFilePropertiesBinding::inflate) {
    private val args by navArgs<PropertiesDialogArgs>()
    override fun onBindViewEvent(binding: DialogFilePropertiesBinding) {
        binding.fullPath.setOnClick {
            requireContext().copyText(it.text)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        val fileInstance = getFileInstance(args.path, requireContext())
        val length = if (fileInstance.isFile) fileInstance.fileLength
        else 0
        binding.model = FileModel(fileInstance.name, fileInstance.path, length, fileInstance.isHidden, fileInstance.fileSystemItem)
        if (fileInstance.isFile) {
            val b = fileInstance.file.extension == "mp4"
            val b1 = fileInstance.file.extension == "mp3"

            binding.videoInfo.isVisible = b
            binding.audoInfo.isVisible = b1
            if (b) {
                val mediaExtractor = MediaExtractor()
                mediaExtractor.setDataSource(fileInstance.path)
                val filter = totalSequence(mediaExtractor.trackCount) {
                    mediaExtractor.getTrackFormat(it)
                }.filter {
                    it.getString(MediaFormat.KEY_MIME)?.startsWith("video") == true
                }.first()
                val width = filter.getInteger(MediaFormat.KEY_WIDTH)
                val height = filter.getInteger(MediaFormat.KEY_HEIGHT)
                val duration = filter.getLong(MediaFormat.KEY_DURATION)
                val sampleRate = filter.getIntOrNull(MediaFormat.KEY_SAMPLE_RATE)
                val frameRate = filter.getIntOrNull(MediaFormat.KEY_FRAME_RATE)
                val colorFormat = filter.getIntOrNull(MediaFormat.KEY_COLOR_FORMAT)
                binding.videoInfo.text = """
                    track count ${mediaExtractor.trackCount}
                    width $width
                    height $height
                    duration $duration ms
                    sample rate: $sampleRate
                    frame rate: $frameRate
                    color format: $colorFormat
                """.trimIndent()

                mediaExtractor.release()
            } else {
                if (b1) {
                    val mediaMetadataRetriever = MediaMetadataRetriever()
                    mediaMetadataRetriever.setDataSource(fileInstance.path)
                    val duration = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    binding.audoInfo.text = "duration: $duration ms"
                }
            }
        }

    }

    private fun MediaFormat.getIntOrNull(key: String) = try {
        getInteger(key)
    } catch (_: Exception) {
        null
    }
}

fun Context.copyText(text: CharSequence) {
    ContextCompat.getSystemService(this, ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("text", text))
    Toast.makeText(this, "copied", Toast.LENGTH_SHORT).show()
}

fun <T : Any> totalSequence(total: Int, block: (Int) -> T): Sequence<T> {
    var start = 0
    return sequence {
        if (start < total) {
            yield(block(start))
            start++
        }
    }
}