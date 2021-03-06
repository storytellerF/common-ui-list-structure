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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding

/**
 * @author storyteller_f
 */

abstract class CommonFragment : Fragment(), RequestFragment, RegistryFragment {
    fun tag(): String = requestKey()
    override fun onStart() {
        super.onStart()
        waitingInFragment.forEach { t ->
            val action = t.value.action as Function2<CommonFragment, Parcelable, Unit>
            if (t.value.requestKey == registryKey()) {
                val callback = { s: String, r: Bundle ->
                    if (waitingInFragment.containsKey(s)) {
                        r.getParcelable<Parcelable>(resultKey)?.let {
                            action(this, it)
                        }
                        waitingInFragment.remove(s)
                    }
                }
                fm.clearFragmentResultListener(t.key)
                fm.setFragmentResultListener(t.key, owner, callback)
            }
        }
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

fun <T : Parcelable, F : CommonFragment> F.fragment(requestKey: String, action: F.(T) -> Unit) {
    val callback = { s: String, r: Bundle ->
        if (waitingInFragment.containsKey(s)) {
            r.getParcelable<T>(resultKey)?.let {
                this.action(it)
            }
            waitingInFragment.remove(s)
        }
    }
    waitingInFragment[requestKey] = FragmentAction(action, registryKey())
    fm.setFragmentResultListener(requestKey, owner, callback)
}

fun <T : Parcelable, F : CommonFragment> F.dialog(dialogFragment: CommonDialogFragment, action: F.(T) -> Unit) {
    val requestKey = dialogFragment.requestKey()
    dialogFragment.show(fm, requestKey)
    val callback = { s: String, r: Bundle ->
        if (waitingInFragment.containsKey(s)) {
            r.getParcelable<T>(resultKey)?.let {
                this.action(it)
            }
            waitingInFragment.remove(s)
        }
    }
    waitingInFragment[requestKey] = FragmentAction(action, registryKey())
    fm.setFragmentResultListener(requestKey, owner, callback)
}

/**
 * fragment ??????toolbar ???????????????
 * 1 ??????activity ???toolbar
 * 2 ??????fragment ?????????toolbar ?????????????????????supportToolbar ????????????
 * RegularFragment ????????????????????????????????????activity ????????????toolbar????????????toolbar ???????????????ComposeView???
 */
abstract class RegularFragment<T : ViewBinding>(
    viewBindingFactory: (LayoutInflater) -> T
) : SimpleFragment<T>(viewBindingFactory) {
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
    get() = when (this) {
        is Fragment -> viewLifecycleOwner
        is ComponentActivity -> this
        else -> throw Exception("unknown type $this")
    }

val LifecycleOwner.fm
    get() = when (this) {
        is Fragment -> parentFragmentManager
        is FragmentActivity -> this.supportFragmentManager
        else -> throw Exception("unknown type $this")
    }

class ActivityAction<T : Parcelable, F : CommonActivity>(val action: F.(T) -> Unit, val requestKey: String)
class FragmentAction<T : Parcelable, F : CommonFragment>(val action: F.(T) -> Unit, val requestKey: String)

val waitingInActivity = mutableMapOf<String, ActivityAction<*, *>>()
val waitingInFragment = mutableMapOf<String, FragmentAction<*, *>>()

interface RequestFragment {
    fun requestKey(): String = this.javaClass.toString()
}

interface RegistryFragment {
    fun registryKey(): String = "${this.javaClass}-registry"
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