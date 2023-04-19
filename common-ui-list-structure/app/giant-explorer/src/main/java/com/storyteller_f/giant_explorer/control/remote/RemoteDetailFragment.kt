package com.storyteller_f.giant_explorer.control.remote

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.hierynomus.smbj.SMBClient
import com.storyteller_f.common_ui.owner
import com.storyteller_f.common_ui.scope
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.common_ui.waitingDialog
import com.storyteller_f.common_vm_ktx.GenericValueModel
import com.storyteller_f.common_vm_ktx.vm
import com.storyteller_f.giant_explorer.database.RemoteAccessSpec
import com.storyteller_f.giant_explorer.database.RemoteSpec
import com.storyteller_f.giant_explorer.database.ShareSpec
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.FragmentRemoteDetailBinding
import com.storyteller_f.giant_explorer.service.FtpInstance
import com.storyteller_f.giant_explorer.service.FtpsInstance
import com.storyteller_f.giant_explorer.service.WebDavInstance
import com.storyteller_f.giant_explorer.service.requireDiskShare
import com.storyteller_f.giant_explorer.service.sftpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object RemoteAccessType {
    const val ftp = "ftp"
    const val sftp = "sftp"
    const val smb = "smb"
    const val ftpes = "ftpes"
    const val ftps = "ftps"
    const val webDav = "webdav"

    val list = listOf("", smb, sftp, ftp, ftpes, ftps, webDav)
}


class RemoteDetailFragment : Fragment() {
    companion object {
        private const val TAG = "RemoteDetailFragment"
    }

    private var _binding: FragmentRemoteDetailBinding? = null

    private val binding get() = _binding!!
    private val model by vm({}) {
        GenericValueModel<RemoteAccessSpec>()
    }

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

        mode.data.observe(owner) {
            Log.i(TAG, "onViewCreated: mode $it")
            binding.shareInput.isVisible = it == RemoteAccessType.smb || it == RemoteAccessType.webDav
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
                        2 -> SMBClient.DEFAULT_PORT.toString()
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
                    when (mode.data.value) {
                        RemoteAccessType.smb -> withContext(Dispatchers.IO) {
                            val requireDiskShare = shareSpec().requireDiskShare()
                            requireDiskShare.close()
                        }

                        RemoteAccessType.ftp -> withContext(Dispatchers.IO) {
                            FtpInstance(spec()).open()
                        }
                        RemoteAccessType.ftpes -> withContext(Dispatchers.IO) {
                            FtpsInstance(spec()).open()
                        }
                        RemoteAccessType.ftps -> withContext(Dispatchers.IO) {
                            FtpsInstance(spec()).open()
                        }
                        RemoteAccessType.webDav -> withContext(Dispatchers.IO) {
                            WebDavInstance(shareSpec()).instance
                        }

                        else -> withContext(Dispatchers.IO) {
                            spec().sftpClient()
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