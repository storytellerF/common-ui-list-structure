package com.storyteller_f.giant_explorer.control.task

import android.net.Uri
import android.os.Parcelable
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import com.storyteller_f.common_ktx.exceptionMessage
import com.storyteller_f.common_pr.observe
import com.storyteller_f.common_ui.*
import com.storyteller_f.giant_explorer.R
import com.storyteller_f.giant_explorer.database.BigTimeTask
import com.storyteller_f.giant_explorer.database.requireDatabase
import com.storyteller_f.giant_explorer.databinding.FragmentAddTaskBinding
import com.storyteller_f.giant_explorer.dialog.RequestPathDialog
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

class AddTaskFragment : SimpleFragment<FragmentAddTaskBinding>(FragmentAddTaskBinding::inflate) {

    override fun onBindViewEvent(binding: FragmentAddTaskBinding) {
        binding.button.setOnClickListener {
            val result = Result(binding.checkBox2.isChecked, binding.path.text.toString().toUri(), binding.selectWorkerName.selectedItem.toString())
            scope.launch {
                val waitingDialog = waitingDialog()
                try {
                    requireDatabase.bigTimeDao().add(BigTimeTask(result.uri, result.enable, result.category))
                    setFragmentResult(result)
                    findNavController().navigateUp()
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), e.exceptionMessage, Toast.LENGTH_SHORT).show()
                } finally {
                    waitingDialog.end()
                }
            }

        }
        binding.selectWorkerName.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, arrayOf("message digest", "torrent name", "folder size"))
        binding.selectPath.setOnClick {
            val requestKey = findNavController().request(R.id.action_select_task_path)
            requestKey.observe(RequestPathDialog.RequestPathResult::class.java) {result ->
                binding.path.setText(result.path)
            }
        }
    }

    @Parcelize
    class Result(val enable: Boolean, val uri: Uri, val category: String) : Parcelable
    companion object

}