package com.storyteller_f.giant_explorer.dialog

import android.util.Log
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.repeatOnLifecycle
import com.storyteller_f.common_ui.CommonDialogFragment
import com.storyteller_f.common_ui.onVisible
import com.storyteller_f.common_ui.pp
import com.storyteller_f.common_ui.scope
import com.storyteller_f.common_vm_ktx.*
import com.storyteller_f.giant_explorer.databinding.DialogFileOperationBinding
import com.storyteller_f.giant_explorer.service.FileOperateBinder
import com.storyteller_f.giant_explorer.service.LocalFileOperateWorker
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.util.*

class FileOperationDialog : CommonDialogFragment<DialogFileOperationBinding>(DialogFileOperationBinding::inflate) {
    lateinit var binder: FileOperateBinder
    private val progressVM by kvm("progress") {
        GenericValueModel<Int>()
    }
    private val leftVM by kvm("left") {
        GenericValueModel<Triple<Int, Int, Long>>()
    }
    private val stateVM by kvm("state") {
        GenericValueModel<String>()
    }
    private val tipVM by kvm("tip") {
        GenericValueModel<String>()
    }
    private val uuid by kavm({ "uuid" }) {
        GenericValueModel<String>().apply {
            data.value = UUID.randomUUID().toString()
        }
    }

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
            Log.i(TAG, "onBindViewEvent: state ${binder.state.value}")
            val key = uuid.data.value ?: return
            if (!binder.map.containsKey(key)) dismiss()
            binder.state.debounce(200).distinctUntilChanged().observe(viewLifecycleOwner) {
                when (it) {
                    FileOperateBinder.state_running -> {
                        val task = binder.map[key]?.taskEquivalent
                        list.onVisible(binding.stateRunning)
                        Log.i(TAG, "onBindViewEvent: $key $task")
                        binding.textViewTask.text = String.format("total size: %d\ntotal file:%d\ntotal folder:%d", task?.size, task?.fileCount, task?.folderCount)
                    }
                    FileOperateBinder.state_end -> {
                        binding.doneText.text = "done"
                        list.onVisible(binding.stateDone)
                    }
                    FileOperateBinder.state_error -> {
                        val task = binder.map[key]
                        binding.doneText.text = task?.message
                        list.onVisible(binding.stateDone)
                    }
                    else -> list.onVisible(binding.stateProgress)
                }
            }
            progressVM.data.observe(viewLifecycleOwner) {
                binding.progressBar.progress = it
            }
            stateVM.data.observe(viewLifecycleOwner) {
                binding.textViewState.text = it
            }
            tipVM.data.observe(viewLifecycleOwner) {
                binding.textViewDetail.text = it
            }
            leftVM.data.observe(viewLifecycleOwner) {
                binding.textViewLeft.text = String.format("size: %d\nleft file:%d\nleft folder:%d", it.third, it.first, it.second)
            }
            val orPut = binder.fileOperationProgressListener.getOrPut(key) { mutableListOf() }
            orPut.add(object : LocalFileOperateWorker.DefaultProgressListener() {
                override fun onProgress(progress: Int, key: String) {
                    progressVM.data.postValue(progress)
                }

                override fun onState(state: String?, key: String) {
                    stateVM.data.postValue(state)
                }

                override fun onTip(tip: String?, key: String) {
                    tipVM.data.postValue(tip)
                }

                override fun onLeft(fileCount: Int, folderCount: Int, size: Long, key: String) {
                    leftVM.data.postValue(fileCount to folderCount to size)
                }

                override fun onComplete(dest: String?, isSuccess: Boolean, key: String) {
                    binding.closeWhenError.pp {
                        it.isVisible = true
                    }
                }

            })
            val callbackFlow = callbackFlow {
                val element = object : LocalFileOperateWorker.DefaultProgressListener() {
                    override fun onDetail(detail: String?, level: Int, key: String) {
                        trySend(detail ?: "null")
                    }
                }
                orPut.add(element)
                awaitClose { orPut.remove(element) }
            }
            val detail = binding.textViewDetail
            scope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    callbackFlow.collect {
                        detail.append(it)
                    }
                }
            }
        } else dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binder.fileOperationProgressListener.clear()
    }

    companion object {
        private const val TAG = "FileOperationDialog"
        const val tag = "file-operation"
    }

    interface Handler {
        fun close()
    }

    override fun requestKey(): String {
        return "file-operation"
    }
}