package com.storyteller_f.common_ui_list_structure

import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.storyteller_f.common_ui.SimpleFragment
import com.storyteller_f.common_ui.dialog
import com.storyteller_f.common_ui.fragment
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.common_ui_list_structure.databinding.FragmentNavigationInvokeBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class NavigationInvokeFragment : SimpleFragment<FragmentNavigationInvokeBinding>(FragmentNavigationInvokeBinding::inflate) {
    override fun onBindViewEvent(binding: FragmentNavigationInvokeBinding) {
        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
            fragment(NavigationResultFragment::class.java.toString(), NavigationResultFragment.Result::class.java) { r ->
                Toast.makeText(requireContext(), "fragment ${r.hh}", Toast.LENGTH_SHORT).show()
            }
        }
        binding.textviewFirst.setOnClick {
            findNavController().navigate(R.id.action_FirstFragment_to_testDialog)
            fragment(TestDialog::class.java.toString(), TestDialog.Result::class.java) { r ->
                Toast.makeText(requireContext(), "fragment-->dialog ${r.test}", Toast.LENGTH_SHORT).show()
            }
        }
        binding.button2.setOnClick {
            dialog(TestDialog2(), TestDialog2.Result::class.java) {r ->
                Toast.makeText(requireContext(), "dialog ${r.test}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}