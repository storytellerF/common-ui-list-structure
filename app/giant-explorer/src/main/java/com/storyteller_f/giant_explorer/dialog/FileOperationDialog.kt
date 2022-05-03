package com.storyteller_f.giant_explorer.dialog

import androidx.lifecycle.distinctUntilChanged
import com.storyteller_f.common_ui.CommonDialogFragment
import com.storyteller_f.common_ui.onVisible
import com.storyteller_f.giant_explorer.databinding.DialogFileOperationBinding
import com.storyteller_f.giant_explorer.service.FileOperateBinder
import com.storyteller_f.giant_explorer.service.FileOperateWorker

class FileOperationDialog : CommonDialogFragment<DialogFileOperationBinding>(DialogFileOperationBinding::inflate) {
    lateinit var binder: FileOperateBinder
    override fun onBindViewEvent(binding: DialogFileOperationBinding) {
        binding.lifecycleOwner = viewLifecycleOwner
        binding.handler = object : Handler {
            override fun close() {
                dismiss()
            }
        }
        dialog?.setCanceledOnTouchOutside(false)
        val list = listOf(binding.stateProgress, binding.stateRunning, binding.stateDone)
        if (::binder.isInitialized) {
            binder.state.distinctUntilChanged().observe(viewLifecycleOwner) {
                when(it) {
                    FileOperateBinder.state_running -> list.onVisible(binding.stateRunning)
                    FileOperateBinder.state_end -> list.onVisible(binding.stateDone)
                    else -> list.onVisible(binding.stateProgress)
                }
            }
            binder.fileOperationProgressListener = object : FileOperateWorker.FileOperationProgressListener {
                override fun onProgress(progress: Int) {
                }

                override fun onState(state: String?) {

                }

                override fun onTip(tip: String?) {

                }

                override fun onDetail(detail: String?, color: Int) {

                }

                override fun onLeft(file_count: Int, folder_count: Int, size: Long) {

                }

                override fun onComplete(dest: String?) {

                }

            }
        }
    }

    interface Handler {
        fun close();
    }
}