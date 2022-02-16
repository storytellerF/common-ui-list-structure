package com.storyteller_f.common_ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 * @author storyteller_f
 */

abstract class CommonFragment<T : ViewBinding>(
    val viewBindingFactory: (LayoutInflater) -> T
) : Fragment() {
    lateinit var binding: T
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = viewBindingFactory(layoutInflater)
        onBindViewEvent(binding)
        return binding.root
    }

    abstract fun onBindViewEvent(binding: T)
}