package com.storyteller_f.giant_explorer.dialog

import android.graphics.Color
import android.os.Parcelable
import android.webkit.MimeTypeMap
import androidx.navigation.fragment.navArgs
import com.j256.simplemagic.ContentInfo
import com.j256.simplemagic.ContentInfoUtil
import com.storyteller_f.common_ui.SimpleDialogFragment
import com.storyteller_f.common_ui.scope
import com.storyteller_f.common_ui.setFragmentResult
import com.storyteller_f.common_vm_ktx.GenericValueModel
import com.storyteller_f.common_vm_ktx.vm
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.giant_explorer.databinding.DialogOpenFileBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.parcelize.Parcelize
import kotlin.concurrent.thread
import kotlin.coroutines.resumeWithException

interface StringResult {
    fun onResult(string: String)
}

class OpenFileDialog : SimpleDialogFragment<DialogOpenFileBinding>(DialogOpenFileBinding::inflate) {
    companion object {
        const val key = "open file"
    }

    private val dataType by vm({}) {
        GenericValueModel<ContentInfo?>()
    }

    private val args by navArgs<OpenFileDialogArgs>()

    @Parcelize
    class OpenFileResult(val mimeType: String) : Parcelable

    override fun onBindViewEvent(binding: DialogOpenFileBinding) {
        val path = args.path
        val fileInstance = FileInstanceFactory.getFileInstance(path, requireContext())
        binding.fileName.text = path
        binding.dataType = dataType
        binding.handler = object : StringResult {
            override fun onResult(string: String) {
                setFragmentResult(OpenFileResult(string))
                dismiss()
            }
        }
        val mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(FileInstance.getExtension(path))
        binding.mimeType = mimeTypeFromExtension
        scope.launch {
            dataType.data.value = suspendCancellableCoroutine {
                thread {
                    try {
                        val value = ContentInfoUtil().findMatch(fileInstance.bufferedInputSteam)
                        it.resumeWith(Result.success(value))
                    } catch (e: Exception) {
                        it.resumeWithException(e)
                    }
                }
            }
        }
        dataType.data.observe(viewLifecycleOwner) {
            binding.openByPicture.setBackgroundColor(mixColor(mimeTypeFromExtension, it, "image"))
            binding.openByText.setBackgroundColor(mixColor(mimeTypeFromExtension, it, "text"))
            binding.openByMusic.setBackgroundColor(mixColor(mimeTypeFromExtension, it, "audio"))
            binding.openByVideo.setBackgroundColor(mixColor(mimeTypeFromExtension, it, "video"))
            binding.openByHex.setBackgroundColor(mixColor(mimeTypeFromExtension, it, "application"))
        }

    }

    private fun mixColor(mimeTypeFromExtension: String?, contentInfo: ContentInfo?, t: String): Int {
        val elements = (if (contentInfo?.contentType?.mimeType?.contains(t) == true) 1 else 2) + if (mimeTypeFromExtension?.contains(t) == true) 4 else 8
        return elements.let {
            when (it) {
                9 -> Color.parseColor("#A25B32")
                6 -> Color.parseColor("#667DDA")
                5 -> Color.parseColor("#D2D205")
                else -> Color.GRAY
            }
        }
    }

    override fun requestKey(): String {
        return key
    }
}

