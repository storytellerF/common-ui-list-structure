package com.storyteller_f.common_ui

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

val LifecycleOwner.scope
    get() = owner.lifecycleScope

val LifecycleOwner.cycle
    @SuppressLint("RestrictedApi")
    get() = owner.lifecycle

fun LifecycleOwner.repeatOnViewResumed(block: suspend CoroutineScope.() -> Unit) {
    scope.launch {
        cycle.repeatOnLifecycle(Lifecycle.State.RESUMED, block)
    }
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
        is FragmentActivity -> supportFragmentManager
        else -> throw Exception("unknown type $this")
    }
