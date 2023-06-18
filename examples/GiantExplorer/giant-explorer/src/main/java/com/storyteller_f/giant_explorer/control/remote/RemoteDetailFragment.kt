package com.storyteller_f.giant_explorer.control.remote

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.storyteller_f.common_pr.state
import com.storyteller_f.common_ui.scope
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.common_ui.waitingDialog
import com.storyteller_f.common_vm_ktx.GenericValueModel
import com.storyteller_f.common_vm_ktx.buildExtras
import com.storyteller_f.common_vm_ktx.vm
import com.storyteller_f.file_system_remote.FtpInstance
import com.storyteller_f.file_system_remote.FtpsInstance
import com.storyteller_f.file_system_remote.RemoteAccessSpec
import com.storyteller_f.file_system_remote.RemoteAccessType
import com.storyteller_f.file_system_remote.RemoteSpec
import com.storyteller_f.file_system_remote.ShareSpec
import com.storyteller_f.file_system_remote.WebDavInstance
import com.storyteller_f.file_system_remote.checkSftp
import com.storyteller_f.file_system_remote.checkSmb
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.FragmentRemoteDetailBinding
import com.storyteller_f.giant_explorer.defaultFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RemoteDetailFragment : Fragment() {
    companion object {
        private const val TAG = "RemoteDetailFragment"
    }

    private var _binding: FragmentRemoteDetailBinding? = null

    private val binding get() = _binding!!
    private val model by viewModels<GenericValueModel<RemoteAccessSpec>>(
        factoryProducer = { defaultFactory },
        extrasProducer = {
            buildExtras {
            }
        })

    //is smb
    private val mode by vm({}) {
        GenericValueModel<String>().apply {
            data.value = RemoteAccessType.smb
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentRemoteDetailBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model
        mode.data.state {
            Log.i(TAG, "onViewCreated: mode $it")
            binding.shareInput.isVisible =
                it == RemoteAccessType.smb || it == RemoteAccessType.webDav
            val i = RemoteAccessType.list.indexOf(it)
            if (binding.typeGroup.checkedRadioButtonId != i) {
                binding.typeGroup.check(i)
            }
        }
        binding.typeGroup.setOnCheckedChangeListener { _, checkedId ->
            Log.i(TAG, "onViewCreated: $checkedId")
            mode.data.value = RemoteAccessType.list[checkedId]
            if (binding.portInput.text.isEmpty()) {
                binding.portInput.setText(
                    when (checkedId - 1) {
                        2 -> "22"
                        0 -> "22"
                        1 -> "22"
                        else -> null
                    }
                )
            }
        }
        binding.testConnection.setOnClick {
            scope.launch {
                waitingDialog {
                    withContext(Dispatchers.IO) {
                        when (mode.data.value) {
                            RemoteAccessType.smb -> shareSpec().checkSmb()
                            RemoteAccessType.ftp -> FtpInstance(spec()).open()
                            RemoteAccessType.ftpes -> FtpsInstance(spec()).open()
                            RemoteAccessType.ftps -> FtpsInstance(spec()).open()
                            RemoteAccessType.webDav -> WebDavInstance(shareSpec()).instance
                            else -> spec().checkSftp()
                        }
                    }

                    Toast.makeText(requireContext(), "success", Toast.LENGTH_SHORT).show()
                }

            }
        }
        binding.buttonSecond.setOnClickListener {
            scope.launch {
                val isSmb = mode.data.value == RemoteAccessType.smb
                withContext(Dispatchers.IO) {
                    val dao = requireDatabase.remoteAccessDao()
                    if (isSmb) dao.add(shareSpec().toRemote()) else dao.add(spec().toRemote())
                }
                Toast.makeText(requireContext(), "success", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun spec(): RemoteSpec {
        return RemoteSpec(
            binding.serverInput.text.toString(),
            binding.portInput.text.toString().toInt(),
            binding.userInput.text.toString(),
            binding.passwordInput.text.toString(),
            mode.data.value.toString()
        )
    }

    private fun shareSpec(): ShareSpec {
        return ShareSpec(
            binding.serverInput.text.toString(),
            binding.portInput.text.toString().toInt(),
            binding.userInput.text.toString(),
            binding.passwordInput.text.toString(),
            mode.data.value.toString(),
            binding.shareInput.text.toString()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}