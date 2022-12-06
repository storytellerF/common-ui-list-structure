package com.storyteller_f.common_ui_list_structure

import androidx.fragment.app.Fragment
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.storyteller_f.common_ui.*
import com.storyteller_f.common_ui_list_structure.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : SimpleFragment<FragmentFirstBinding>(FragmentFirstBinding::inflate) {
    override fun onBindViewEvent(binding: FragmentFirstBinding) {
        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
            fragment(SecondFragment::class.java.toString()) { r: SecondFragment.Result ->
                Toast.makeText(requireContext(), "fragment ${r.hh}", Toast.LENGTH_SHORT).show()
            }
        }
        binding.textviewFirst.setOnClick {
            findNavController().navigate(R.id.action_FirstFragment_to_testDialog)
            fragment(TestDialog::class.java.toString()) { r: TestDialog.Result ->
                Toast.makeText(requireContext(), "fragment-->dialog ${r.test}", Toast.LENGTH_SHORT).show()
            }
        }
        binding.button2.setOnClick {
            dialog(TestDialog2()) { r: TestDialog2.Result ->
                Toast.makeText(requireContext(), "dialog ${r.test}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}