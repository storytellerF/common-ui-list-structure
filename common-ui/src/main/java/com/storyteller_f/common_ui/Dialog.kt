package com.storyteller_f.common_ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

abstract class CommonDialogFragment : DialogFragment(), RequestFragment {
    fun tag(): String = requestKey()
}

/**
 * @param viewBindingFactory inflate
 */
abstract class SimpleDialogFragment<T : ViewBinding>(
    val viewBindingFactory: (LayoutInflater) -> T
) : CommonDialogFragment() {
    private var _binding: T? = null
    val binding: T get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val bindingLocal = viewBindingFactory(layoutInflater)
        _binding = bindingLocal
        (bindingLocal as? ViewDataBinding)?.lifecycleOwner = viewLifecycleOwner
        onBindViewEvent(binding)
        return bindingLocal.root
    }

    abstract fun onBindViewEvent(binding: T)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (binding as? ViewDataBinding)?.lifecycleOwner = viewLifecycleOwner
    }
}


class WaitingDialog : DialogFragment(R.layout.dialog_waiting) {
    lateinit var deferred: CompletableDeferred<Unit>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (::deferred.isInitialized) {
            lifecycleScope.launch {
                deferred.await()
                dismiss()
            }
        }
    }
}

fun Fragment.waitingDialog(): CompletableDeferred<Unit> {
    val deferred = CompletableDeferred<Unit>()
    WaitingDialog().apply {
        this.deferred = deferred
    }.show(fm, "waiting")
    return deferred
}

fun CompletableDeferred<Unit>.end() = this.complete(Unit)