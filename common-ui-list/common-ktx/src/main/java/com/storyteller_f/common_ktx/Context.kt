package com.storyteller_f.common_ktx

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner

fun <T> LifecycleOwner.context(function: Context.() -> T) = (when (this) {
    is ComponentActivity -> {
        this
    }
    is Fragment -> {
        requireContext()
    }
    else -> throw Exception("context is null")
}).run(function)

val LifecycleOwner.ctx
    get() = when (this) {
        is ComponentActivity -> this
        is Fragment -> requireContext()
        else -> throw Exception("unknown type $this")
    }
