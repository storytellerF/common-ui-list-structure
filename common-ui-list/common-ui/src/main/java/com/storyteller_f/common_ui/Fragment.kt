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
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 * @author storyteller_f
 */

abstract class CommonFragment : Fragment(), ResponseFragment, Registry {

    override val vm by responseModel

    override fun onStart() {
        super.onStart()
        observeResponse()
    }
}

abstract class SimpleFragment<T : ViewBinding>(
    val viewBindingFactory: (LayoutInflater) -> T
) : CommonFragment() {
    private var _binding: T? = null
    val binding: T get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val bindingLocal = viewBindingFactory(layoutInflater)
        _binding = bindingLocal
        (binding as? ViewDataBinding)?.lifecycleOwner = viewLifecycleOwner
        onBindViewEvent(binding)
        return bindingLocal.root
    }

    abstract fun onBindViewEvent(binding: T)
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
) : SimpleFragment<T>(viewBindingFactory) {
    override fun onStart() {
        super.onStart()
        toolbar.setDisplayHomeAsUpEnabled(up())
        toolbarCompose.setContent { }
    }

    open fun up() = true
}

val Fragment.toolbar: ActionBar
    get() = (activity as AppCompatActivity).supportActionBar!!

val Fragment.toolbarCompose
    get() = (activity!!.findViewById<Toolbar>(R.id.toolbar)).getChildAt(0) as
            ComposeView
