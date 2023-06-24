package com.storyteller_f.common_ui_list_structure.test_navigation

import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.storyteller_f.common_ui.SimpleFragment
import com.storyteller_f.common_ui.dialog
import com.storyteller_f.common_ui.fragment
import com.storyteller_f.common_ui.request
import com.storyteller_f.common_ui.requestDialog
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.common_ui_list_structure.R
import com.storyteller_f.common_ui_list_structure.dialog.TestDialog2
import com.storyteller_f.common_ui_list_structure.databinding.FragmentNavigationInvokeBinding
import com.storyteller_f.common_ui_list_structure.dialog.NavigationDialog

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class NavigationInvokeFragment : SimpleFragment<FragmentNavigationInvokeBinding>(FragmentNavigationInvokeBinding::inflate) {
    override fun onBindViewEvent(binding: FragmentNavigationInvokeBinding) {
        binding.buttonFirst.setOnClickListener {
            val requestKey = findNavController().request(R.id.action_FirstFragment_to_SecondFragment)
            fragment(requestKey, NavigationResultFragment.Result::class.java) { r ->
                Toast.makeText(requireContext(), "fragment： ${r.hh}", Toast.LENGTH_SHORT).show()
            }
        }
        binding.textviewFirst.setOnClick {
            val request = findNavController().request(R.id.action_FirstFragment_to_testDialog)
            fragment(request, NavigationDialog.Result::class.java) { r ->
                Toast.makeText(requireContext(), "fragment-->dialog ${r.test}", Toast.LENGTH_SHORT).show()
            }
        }
        binding.button2.setOnClick {
            dialog(TestDialog2.Result::class.java, requestDialog(TestDialog2::class.java),) { r ->
                Toast.makeText(requireContext(), "dialog ${r.test}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}