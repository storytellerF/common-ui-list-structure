package com.storyteller_f.common_ui

import android.os.Bundle
import android.view.KeyEvent
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

/**
 * @param viewBindingFactory inflate
 */
abstract class CommonDialogFragment<T : ViewBinding>(
    val viewBindingFactory: (LayoutInflater) -> T
) : DialogFragment(), KeyEvent.Callback {
    var binding: T? = null
    abstract fun requestKey(): String
    fun tag():String = requestKey()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val bindingLocal = viewBindingFactory(layoutInflater)
        binding = bindingLocal
        (bindingLocal as? ViewDataBinding)?.lifecycleOwner = viewLifecycleOwner
        onBindViewEvent(bindingLocal)
        return bindingLocal.root
    }

    abstract fun onBindViewEvent(binding: T)

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (binding as? ViewDataBinding)?.lifecycleOwner = viewLifecycleOwner
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?) = false

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?) = false

    override fun onKeyUp(keyCode: Int, event: KeyEvent?) = false

    override fun onKeyMultiple(keyCode: Int, count: Int, event: KeyEvent?) = false
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
    }.show(parentFragmentManager, "waiting")
    return deferred
}

fun CompletableDeferred<Unit>.end() = this.complete(Unit)