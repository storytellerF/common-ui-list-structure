package com.storyteller_f.common_ui_list_structure

import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.storyteller_f.common_ui.CommonFragment
import com.storyteller_f.common_ui.setFragmentResult
import com.storyteller_f.common_ui_list_structure.databinding.FragmentSecondBinding
import kotlinx.parcelize.Parcelize

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : CommonFragment<FragmentSecondBinding>(FragmentSecondBinding::inflate) {

    override fun onBindViewEvent(binding: FragmentSecondBinding) {
        binding.buttonSecond.setOnClickListener {
//            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
            setFragmentResult(Result("hello world!"))
            findNavController().navigateUp()
        }
    }

    @Parcelize
    class Result(val hh: String) : Parcelable
}