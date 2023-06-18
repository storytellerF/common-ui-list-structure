package com.storyteller_f.common_ui

import android.annotation.SuppressLint
import android.os.Build
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
            @Suppress("UNCHECKED_CAST") val action = t.value.action
            if (t.value.registerKey == registryKey()) {
                val callback = { s: String, r: Bundle ->
                    if (waitingInFragment.containsKey(s)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            r.getParcelable(fragmentResultKey, Parcelable::class.java)?.let {
                                action(this, it)
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            r.getParcelable<Parcelable>(fragmentResultKey)?.let {
                                action(this, it)
                            }
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

fun <T : Parcelable, F> F.fragment(requestKey: String, result: Class<T>,  action: F.(T) -> Unit) where F : CommonFragment, F : RegistryFragment {
    val callback = { s: String, r: Bundle ->
        if (waitingInFragment.containsKey(s)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                r.getParcelable(fragmentResultKey, result)?.let {
                    action(it)
                }
            } else {
                @Suppress("DEPRECATION")
                r.getParcelable<T>(fragmentResultKey)?.let {
                    action(it)
                }
            }
            waitingInFragment.remove(s)
        }
    }
    @Suppress("UNCHECKED_CAST")
    waitingInFragment[requestKey] = FragmentAction(action as (CommonFragment, Parcelable) -> Unit, registryKey())
    fm.setFragmentResultListener(requestKey, owner, callback)
}

fun <T : Parcelable, F> F.dialog(dialogFragment: CommonDialogFragment, result: Class<T>, action: F.(T) -> Unit) where F : Fragment, F : RegistryFragment {
    val requestKey = dialogFragment.requestKey()
    dialogFragment.show(fm, requestKey)
    val callback = { s: String, r: Bundle ->
        if (waitingInFragment.containsKey(s)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                r.getParcelable(fragmentResultKey, result)?.let {
                    action(it)
                }
            } else {
                @Suppress("DEPRECATION")
                r.getParcelable<T>(fragmentResultKey)?.let {
                    action(it)
                }
            }
            waitingInFragment.remove(s)
        }
    }
    @Suppress("UNCHECKED_CAST")
    waitingInFragment[requestKey] = FragmentAction(action as (CommonFragment, Parcelable) -> Unit, registryKey())
    fm.setFragmentResultListener(requestKey, owner, callback)
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

class ActivityAction(val action: (CommonActivity, Parcelable) -> Unit, val registerKey: String)
class FragmentAction(val action: (CommonFragment, Parcelable) -> Unit, val registerKey: String)

val waitingInActivity = mutableMapOf<String, ActivityAction>()
val waitingInFragment = mutableMapOf<String, FragmentAction>()

/**
 * 可以被请求。返回的结构通过requestKey 辨别
 */
interface RequestFragment {
    /**
     * 用于setFragmentResultListener 接受结果
     */
    fun requestKey(): String = this.javaClass.toString()
}

/**
 * 可以发起请求，发起的请求通过registryKey 辨别
 */
interface RegistryFragment {
    /**
     * 代表当前关心的事件
     */
    fun registryKey(): String = "${this.javaClass}-registry"
}

fun <T : Parcelable> Fragment.setFragmentResult(requestKey: String, result: T) {
    fm.setFragmentResult(requestKey, Bundle().apply {
        putParcelable(fragmentResultKey, result)
    })
}

fun <T, F> F.setFragmentResult(result: T) where T : Parcelable, F : Fragment, F : RequestFragment {
    val requestKey = requestKey()
    setFragmentResult(requestKey, result)
}

val fragmentResultKey
    get() = "result"