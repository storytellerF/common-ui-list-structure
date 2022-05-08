package com.storyteller_f.common_ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ComponentActivity
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding

/**
 * @author storyteller_f
 */

abstract class CommonFragment<T : ViewBinding>(
    val viewBindingFactory: (LayoutInflater) -> T
) : Fragment(), RequestFragment {
    private var _binding: T? = null
    val binding: T get() = _binding!!
    fun tag(): String = requestKey()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val bindingLocal = viewBindingFactory(layoutInflater)
        _binding = bindingLocal
        (binding as? ViewDataBinding)?.lifecycleOwner = viewLifecycleOwner
        onBindViewEvent(bindingLocal)
        return bindingLocal.root
    }

    abstract fun onBindViewEvent(binding: T)

    override fun onStart() {
        super.onStart()
        waitingInFragment.forEach { t ->
            fm.clearFragmentResult(t.key)
            fm.setFragmentResultListener(t.key, owner, t.value.action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun <T : Parcelable> fragment(requestKey: String, action: (T) -> Unit) {
        val callback = { s: String, r: Bundle ->
            if (waitingInFragment.containsKey(s)) {
                r.getParcelable<T>(resultKey)?.let {
                    action.invoke(it)
                }
                waitingInFragment.remove(s)
            }
        }
        waitingInFragment[requestKey] = FragmentAction(callback)
        fm.setFragmentResultListener(requestKey, owner, callback)
    }

    fun <T : Parcelable> dialog(dialogFragment: CommonDialogFragment<*>, action: (T) -> Unit) {
        val requestKey = dialogFragment.requestKey()
        dialogFragment.show(fm, requestKey)
        val callback = { s: String, r: Bundle ->
            if (waitingInFragment.containsKey(s)) {
                r.getParcelable<T>(resultKey)?.let {
                    action.invoke(it)
                }
                waitingInFragment.remove(s)
            }
        }
        waitingInFragment[requestKey] = FragmentAction(callback)
        fm.setFragmentResultListener(requestKey, owner, callback)
    }

    companion object {
        private const val TAG = "Fragment"
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

val LifecycleOwner.scope
    get() = when (this) {
        is Fragment -> viewLifecycleOwner.lifecycleScope
        is ComponentActivity -> lifecycleScope
        else -> throw Exception("unknown type $this")
    }

val LifecycleOwner.cycle
    @SuppressLint("RestrictedApi")
    get() = when (this) {
        is Fragment -> viewLifecycleOwner.lifecycle
        is ComponentActivity -> lifecycle
        else -> throw Exception("unknown type $this")
    }

val LifecycleOwner.owner
    @SuppressLint("RestrictedApi")
    get() = when (this) {
        is Fragment -> viewLifecycleOwner
        is ComponentActivity -> this
        else -> throw Exception("unknown type $this")
    }

val Fragment.fm
    get() = parentFragmentManager

class FragmentAction(val action: (String, Bundle) -> Unit)

val waitingInActivity = mutableMapOf<String, FragmentAction>()
val waitingInFragment = mutableMapOf<String, FragmentAction>()

interface RequestFragment {
    fun requestKey(): String = this.javaClass.toString()
}

fun <T : Parcelable> Fragment.setFragmentResult(requestKey: String, result: T) {
    fm.setFragmentResult(requestKey, Bundle().apply {
        putParcelable(resultKey, result)
    })
}

fun <T, F> F.setFragmentResult(result: T) where T : Parcelable, F : Fragment, F : RequestFragment {
    val requestKey = requestKey()
    fm.setFragmentResult(requestKey, Bundle().apply {
        putParcelable(resultKey, result)
    })
}

val Fragment.resultKey
        get() = "result"