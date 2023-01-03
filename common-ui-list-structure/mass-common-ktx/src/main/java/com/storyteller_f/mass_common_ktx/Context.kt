package com.storyteller_f.mass_common_ktx

import android.content.Context
import android.view.View
import androidx.core.app.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.work.ListenableWorker

fun <T, R> T.contextAny(function: Context.() -> R) = (when (this) {
    is ComponentActivity -> this
    is Fragment -> requireContext()
    is ListenableWorker -> applicationContext
    is View -> context
    else -> throw java.lang.Exception("context is null")
}).run(function)