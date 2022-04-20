package com.storyteller_f.common_ui

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ComponentActivity
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding

/**
 * @author storyteller_f
 */

abstract class CommonFragment<T : ViewBinding>(
    val viewBindingFactory: (LayoutInflater) -> T
) : Fragment(), KeyEvent.Callback {
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

    override fun onStart() {
        super.onStart()
    }

    operator fun set(requestKey: String, action: (Bundle) -> Unit) {
        waiting[requestKey] = action
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?) = false

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?) = false

    override fun onKeyUp(keyCode: Int, event: KeyEvent?) = false

    override fun onKeyMultiple(keyCode: Int, count: Int, event: KeyEvent?) = false
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
) : DialogFragment(), KeyEvent.Callback {
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?) = false

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?) = false

    override fun onKeyUp(keyCode: Int, event: KeyEvent?) = false

    override fun onKeyMultiple(keyCode: Int, count: Int, event: KeyEvent?) = false
}

val Fragment.scope get() = viewLifecycleOwner.lifecycleScope

val waiting = mutableMapOf<String, (Bundle) -> Unit>()

/**
 * 3 代表支持三种类型
 */
fun<T> KeyEvent.Callback.context3(function: Context.() -> T) = (when (this) {
    is ComponentActivity -> this
    is Fragment -> requireContext()
    is View -> context
    else -> throw java.lang.Exception("context is null")
}).run(function)

suspend fun<T> KeyEvent.Callback.contextSuspend3(function: suspend Context.() -> T): T = when (this) {
    is ComponentActivity -> this
    is Fragment -> requireContext()
    else -> throw java.lang.Exception("context is null")
}.function()
