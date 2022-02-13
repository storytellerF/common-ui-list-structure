package com.storyteller_f.common_ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment

/**
 * @author storyteller_f
 */

abstract class CommonFragment(@LayoutRes private val layoutId: Int) : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val inflate = inflater.inflate(layoutId, container)!!
        onBindViewEvent(inflate)
        return inflate
    }

    abstract fun onBindViewEvent(inflate: View)
}