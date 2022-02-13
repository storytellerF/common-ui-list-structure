package com.storyteller_f.common_ui_list_structure.ui.home

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import com.storyteller_f.common_ui.CommonFragment
import com.storyteller_f.common_ui_list_structure.R
import com.storyteller_f.common_ui_list_structure.databinding.FragmentHomeBinding
import com.storyteller_f.common_ui_list_structure.model.Repo
import com.storyteller_f.common_vm_ktx.gor
import com.storyteller_f.common_vm_ktx.plus
import com.storyteller_f.ui_list.event.viewBinding

class HomeFragment : CommonFragment(R.layout.fragment_home) {

    private val binding by viewBinding(FragmentHomeBinding::bind)
    private val homeViewModel by viewModels<HomeViewModel>()

    override fun onBindViewEvent(inflate: View) {
        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val l1 = MutableLiveData(0)
        val l2 = MutableLiveData(true)
        val l3 = MutableLiveData<Repo>(null)
        l1.plus(l2).plus(l3).observe(viewLifecycleOwner) {
            val data = it as List<Any?>
            val age = data.gor(0) as Int?
            val gender = data.gor(1) as Boolean?
            val repo = data.gor(2) as Repo?
            println("hello $age $gender $repo")
        }
        l1.value = 2
        l2.value = false
        l3.value = Repo(0, "", "", "", "", 12, 15, "")
    }

}