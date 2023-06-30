package com.storyteller_f.giant_explorer.dialog

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.repeatOnLifecycle
import com.storyteller_f.common_pr.state
import com.storyteller_f.common_ui.SimpleDialogFragment
import com.storyteller_f.common_ui.onVisible
import com.storyteller_f.common_ui.owner
import com.storyteller_f.common_ui.pp
import com.storyteller_f.common_ui.scope
import com.storyteller_f.common_vm_ktx.GenericValueModel
import com.storyteller_f.common_vm_ktx.avm
import com.storyteller_f.common_vm_ktx.debounce
import com.storyteller_f.common_vm_ktx.keyPrefix
import com.storyteller_f.common_vm_ktx.to
import com.storyteller_f.common_vm_ktx.vm
import com.storyteller_f.file_system.operate.DefaultForemanProgressListener
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.databinding.DialogFileOperationBinding
import com.storyteller_f.giant_explorer.service.FileOperateBinder
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.util.UUID

class FileOperationDialog :
    SimpleDialogFragment<DialogFileOperationBinding>(DialogFileOperationBinding::inflate) {
    lateinit var binder: FileOperateBinder

    private val progressVM by keyPrefix({ "progress" }, vm({}) {
        GenericValueModel<Int>()
    })
    private val leftVM by keyPrefix("left", vm({}) {
        GenericValueModel<Triple<Int, Int, Long>>().apply {
            data.value = -1 to -1 to -1
        }
    })
    private val stateVM by keyPrefix("state", vm({}) {
        GenericValueModel<String>()
    })
    private val tipVM by keyPrefix("tip", vm({}) {
        GenericValueModel<String>()
    })
    private val uuid by keyPrefix({ "uuid" }, avm({}) {
        GenericValueModel<String>().apply {
            Log.i(TAG, "uuid: new")
            data.value = UUID.randomUUID().toString()
        }
    })

    override fun onBindViewEvent(binding: DialogFileOperationBinding) {

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = owner
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
            bindUi(key, list, binding)
            bindListener(key, binding)
        } else dismiss()
    }

    private fun bindListener(key: String, binding: DialogFileOperationBinding) {
        val orPut = binder.fileOperationProgressListener.getOrPut(key) { mutableListOf() }
        orPut.add(object : DefaultForemanProgressListener() {
            override fun onProgress(progress: Int, key: String) =
                progressVM.data.postValue(progress)

            override fun onState(state: String?, key: String) = stateVM.data.postValue(state)

            override fun onTip(tip: String?, key: String) = tipVM.data.postValue(tip)

            override fun onLeft(fileCount: Int, folderCount: Int, size: Long, key: String) =
                leftVM.data.postValue(fileCount to folderCount to size)

            override fun onComplete(dest: String?, isSuccess: Boolean, key: String) {
                binding.closeWhenError.pp {
                    it.isVisible = true
                }
            }

        })
        val callbackFlow = callbackFlow {
            val defaultProgressListener = object : DefaultForemanProgressListener() {
                override fun onDetail(detail: String?, level: Int, key: String) {
                    trySend(detail ?: "null")
                }
            }
            orPut.add(defaultProgressListener)
            awaitClose { orPut.remove(defaultProgressListener) }
        }
        val detail = binding.textViewDetail
        scope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                callbackFlow.collect {
                    detail.append(it)
                }
            }
        }
    }

    private fun bindUi(key: String, list: List<View>, binding: DialogFileOperationBinding) {
        binder.state.debounce(500).distinctUntilChanged().observe(owner) {
            when (it) {
                FileOperateBinder.state_running -> {
                    val task = binder.map[key]?.taskAssessResult
                    list.onVisible(binding.stateRunning)
                    Log.i(TAG, "onBindViewEvent: $key $task ${binder.map.keys}")
                    binding.textViewTask.text = String.format(
                        "total size: %d\ntotal file:%d\ntotal folder:%d",
                        task?.size,
                        task?.fileCount,
                        task?.folderCount
                    )
                }

                FileOperateBinder.state_end -> {
                    binding.doneText.text = getString(R.string.operation_task_done)
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
        progressVM.data.state {
            binding.progressBar.progress = it
        }
        stateVM.data.state {
            binding.textViewState.text = it
        }
        tipVM.data.state {
            binding.textViewDetail.text = it
        }
        leftVM.data.state {
            Log.i(TAG, "onBindViewEvent: leftVM: $it")
            binding.textViewLeft.text = presentTaskSnapshot(it)
        }
    }

    private fun presentTaskSnapshot(it: Triple<Int, Int, Long>) =
        String.format("size: %d\nleft file:%d\nleft folder:%d", it.third, it.first, it.second)

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
}