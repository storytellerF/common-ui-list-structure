package com.storyteller_f.giant_explorer.control.remote

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.storyteller_f.common_ktx.exceptionMessage
import com.storyteller_f.common_ui.scope
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.FragmentRemoteDetailBinding
import com.storyteller_f.giant_explorer.service.FtpInstance
import com.storyteller_f.giant_explorer.service.FtpSpec
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentRemoteDetailBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.testConnection.setOnClick {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        FtpInstance(spec()).open()
                    }
                    Toast.makeText(requireContext(), "success", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), e.exceptionMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
        binding.buttonSecond.setOnClickListener {
            scope.launch {
                withContext(Dispatchers.IO) {
                    requireDatabase.remoteAccessDao().add(spec().toRemote())
                }
                Toast.makeText(requireContext(), "success", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun spec(): FtpSpec {
        return FtpSpec(binding.serverInput.text.toString(), binding.portInput.text.toString().toInt(), binding.userInput.text.toString(), binding.passwordInput.text.toString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}