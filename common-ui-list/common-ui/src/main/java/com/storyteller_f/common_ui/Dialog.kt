package com.storyteller_f.common_ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.storyteller_f.common_ktx.ctx
import com.storyteller_f.common_ktx.exceptionMessage
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

fun LifecycleOwner.waitingDialog(): CompletableDeferred<Unit> {
    val deferred = CompletableDeferred<Unit>()
    WaitingDialog().apply {
        this.deferred = deferred
    }.show(fm, "waiting")
    return deferred
}


fun LifecycleOwner.waitingDialog(block: suspend () -> Unit) {
    scope.launch {
        val waiting = waitingDialog()
        try {
            block()
        } catch (e: Exception) {
            Toast.makeText(ctx, e.exceptionMessage, Toast.LENGTH_SHORT).show()
        } finally {
            waiting.end()
        }
    }
}

fun CompletableDeferred<Unit>.end() = this.complete(Unit)