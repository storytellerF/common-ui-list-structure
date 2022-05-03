package com.storyteller_f.giant_explorer.control

import android.os.Parcelable
import androidx.fragment.app.Fragment
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.storyteller_f.common_ui.*
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.database.BigTimeTask
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.FragmentSecondBinding
import com.storyteller_f.giant_explorer.dialog.RequestPathDialog
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.lang.Exception

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : CommonFragment<FragmentSecondBinding>(FragmentSecondBinding::inflate) {

    override fun onBindViewEvent(binding: FragmentSecondBinding) {
        binding.button.setOnClickListener {
            val result = Result(binding.checkBox2.isChecked, binding.path.text.toString(), binding.selectWorkerName.selectedItem.toString())
            scope.launch {
                val waitingDialog = waitingDialog()
                try {
                    requireDatabase().bigTimeDao().add(BigTimeTask(result.path, result.enable, result.workerName))
                    setFragmentResult(requestKey, result)
                    findNavController().navigateUp()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), e.localizedMessage ?: e.javaClass.toString(), Toast.LENGTH_SHORT).show()
                } finally {
                    waitingDialog.end()
                }
            }

        }
        binding.selectWorkerName.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, arrayOf("message digest", "torrent name", "folder size"))
        binding.selectPath.setOnClick {
            findNavController().navigate(R.id.action_SecondFragment_to_requestPathDialog)
            fragment<RequestPathDialog.RequestPathResult>(RequestPathDialog.requestKey) {
                binding.path.setText(it.path)
            }
        }
    }

    @Parcelize
    class Result(val enable: Boolean, val path: String, val workerName: String) : Parcelable
    companion object {
        const val requestKey = "test"
    }
}