package com.storyteller_f.common_ui_list_structure

import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.storyteller_f.common_ui.SimpleFragment
import com.storyteller_f.common_ui.setFragmentResult
import com.storyteller_f.common_ui_list_structure.databinding.FragmentNavigationResultBinding
import kotlinx.parcelize.Parcelize

/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class NavigationResultFragment : SimpleFragment<FragmentNavigationResultBinding>(FragmentNavigationResultBinding::inflate) {

    override fun onBindViewEvent(binding: FragmentNavigationResultBinding) {
        binding.buttonSecond.setOnClickListener {
//            findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment)
            setFragmentResult(Result("second fragment"))
            findNavController().navigateUp()
        }
    }

    @Parcelize
    class Result(val hh: String) : Parcelable
}