package com.storyteller_f.giant_explorer.dialog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.navArgs
import com.storyteller_f.common_ui.SimpleDialogFragment
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.giant_explorer.control.getFileInstance
import com.storyteller_f.giant_explorer.databinding.DialogFilePropertiesBinding
import com.storyteller_f.giant_explorer.model.FileModel
import kotlin.time.Duration

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
            if (fileInstance.file.extension == "mp4") {
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
                binding.videoInfo.text = """
                    track count ${mediaExtractor.trackCount}
                    width $width
                    height $height
                    duration $duration ms
                """.trimIndent()

                mediaExtractor.release()
            }
        }

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