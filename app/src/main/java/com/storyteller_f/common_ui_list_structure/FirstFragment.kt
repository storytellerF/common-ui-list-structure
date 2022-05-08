package com.storyteller_f.common_ui_list_structure

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.storyteller_f.common_ui.CommonFragment
import com.storyteller_f.common_ui.setOnClick
import com.storyteller_f.common_ui_list_structure.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : CommonFragment<FragmentFirstBinding>(FragmentFirstBinding::inflate) {
    override fun onBindViewEvent(binding: FragmentFirstBinding) {
        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
            fragment<SecondFragment.Result>(SecondFragment::class.java.toString()) {
                Toast.makeText(requireContext(), it.hh, Toast.LENGTH_SHORT).show()
            }
        }
        binding.textviewFirst.setOnClick {
            findNavController().navigate(R.id.action_FirstFragment_to_testDialog)
            fragment<TestDialog.Result>(TestDialog::class.java.toString()) {
                Toast.makeText(requireContext(), it.test, Toast.LENGTH_SHORT).show()
            }
        }
        binding.button2.setOnClick {
            dialog<TestDialog2.Result>(TestDialog2()) {
                Toast.makeText(requireContext(), it.test, Toast.LENGTH_SHORT).show()
            }
        }
    }
}