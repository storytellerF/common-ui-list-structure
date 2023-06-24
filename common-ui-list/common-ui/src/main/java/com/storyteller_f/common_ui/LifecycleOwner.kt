package com.storyteller_f.common_ui

import android.annotation.SuppressLint
import androidx.core.app.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope


val LifecycleOwner.scope
    get() = owner.lifecycleScope

val LifecycleOwner.cycle
    @SuppressLint("RestrictedApi")
    get() = owner.lifecycle

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