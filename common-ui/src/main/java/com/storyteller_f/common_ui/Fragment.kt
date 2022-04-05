package com.storyteller_f.common_ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.compose.ui.platform.ComposeView
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import kotlin.reflect.KFunction

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (binding as? ViewDataBinding)?.lifecycleOwner = viewLifecycleOwner
    }
}

/**
 * fragment 中的toolbar 有两种情况
 * 1 使用activity 的toolbar
 * 2 使用fragment 自己的toolbar ，这时不能使用supportToolbar 等方法。
 * RegularFragment 使用的是第一种方法，假定activity 含有一个toolbar，且这个toolbar 是包含一个ComposeView的
 */
abstract class RegularFragment<T : ViewBinding>(
    viewBindingFactory: (LayoutInflater) -> T

) : CommonFragment<T>(viewBindingFactory) {
    override fun onStart() {
        super.onStart()
        toolbar().setDisplayHomeAsUpEnabled(up())
        toolbarCompose().setContent { }
    }

    open fun up() = true
}

fun Fragment.toolbar(): ActionBar = (activity as AppCompatActivity).supportActionBar!!

fun Fragment.toolbarCompose() =
    (activity!!.findViewById<Toolbar>(R.id.toolbar)).getChildAt(0) as
            ComposeView

abstract class CommonDialogFragment<T : ViewBinding>(
    val viewBindingFactory: (LayoutInflater) -> T
) : DialogFragment() {
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (binding as? ViewDataBinding)?.lifecycleOwner = viewLifecycleOwner
    }
}

val Fragment.scope get() = viewLifecycleOwner.lifecycleScope

val waiting = mutableMapOf<String, (Bundle) -> Unit>()
