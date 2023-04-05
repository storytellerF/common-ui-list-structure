package com.storyteller_f.giant_explorer.control.remote

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.distinctUntilChanged
import com.hierynomus.smbj.SMBClient
import com.storyteller_f.common_ktx.exceptionMessage
import com.storyteller_f.common_ui.owner
import com.storyteller_f.common_ui.scope
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.common_ui.waitingDialog
import com.storyteller_f.common_vm_ktx.GenericValueModel
import com.storyteller_f.common_vm_ktx.vm
import com.storyteller_f.giant_explorer.database.RemoteAccessSpec
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.FragmentRemoteDetailBinding
import com.storyteller_f.giant_explorer.service.FtpInstance
import com.storyteller_f.giant_explorer.service.FtpSpec
import com.storyteller_f.giant_explorer.service.SmbSpec
import com.storyteller_f.giant_explorer.service.requireDiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class RemoteDetailFragment : Fragment() {

    private var _binding: FragmentRemoteDetailBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private val model by vm({}) {
        GenericValueModel<RemoteAccessSpec>()
    }

    //is smb
    private val mode by vm({}) {
        GenericValueModel<Boolean>()
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
            binding.shareInput.isVisible = it
        }
        binding.smbCheckbox.setOnCheckedChangeListener { buttonView, isChecked ->
            mode.data.value = isChecked
            if (isChecked) {
                if (binding.portInput.text.isEmpty()) {
                    binding.portInput.setText(SMBClient.DEFAULT_PORT.toString())
                }
            }
        }
        mode.data.distinctUntilChanged().observe(owner) {
            binding.smbCheckbox.isChecked = it
        }
        binding.testConnection.setOnClick {
            scope.launch {
                val isSmb = mode.data.value == true
                if (isSmb) {
                    waitingDialog {
                        withContext(Dispatchers.IO) {
                            val requireDiskShare = smbSpec().requireDiskShare()
                            requireDiskShare.close()
                        }
                        Toast.makeText(requireContext(), "success", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    waitingDialog {
                        withContext(Dispatchers.IO) {
                            FtpInstance(spec()).open()
                        }
                        Toast.makeText(requireContext(), "success", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        binding.buttonSecond.setOnClickListener {
            scope.launch {
                val isSmb = mode.data.value == true
                withContext(Dispatchers.IO) {
                    val dao = requireDatabase.remoteAccessDao()
                    if (isSmb) dao.add(smbSpec().toRemote()) else dao.add(spec().toRemote())
                }
                Toast.makeText(requireContext(), "success", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun spec(): FtpSpec {
        return FtpSpec(binding.serverInput.text.toString(), binding.portInput.text.toString().toInt(), binding.userInput.text.toString(), binding.passwordInput.text.toString())
    }

    private fun smbSpec(): SmbSpec {
        return SmbSpec(
            binding.serverInput.text.toString(),
            binding.portInput.text.toString().toInt(),
            binding.userInput.text.toString(),
            binding.passwordInput.text.toString(),
            binding.shareInput.text.toString()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}