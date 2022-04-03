package com.storyteller_f.common_ktx

import android.content.Context
import androidx.core.app.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner

fun<T> LifecycleOwner.context(function: Context.() -> T) = (when (this) {
    is ComponentActivity -> {
        this
    }
    is Fragment -> {
        requireContext()
    }
    else -> throw java.lang.Exception("context is null")
}).run(function)

suspend fun<T> LifecycleOwner.contextSuspend(function: suspend Context.() -> T): T = when (this) {
    is ComponentActivity -> {
        this
    }
    is Fragment -> {
        requireContext()
    }
    else -> throw java.lang.Exception("context is null")
}.function()