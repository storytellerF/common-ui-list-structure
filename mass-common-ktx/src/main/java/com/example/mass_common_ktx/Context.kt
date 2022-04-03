package com.example.mass_common_ktx

import android.content.Context
import androidx.core.app.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.work.ListenableWorker

fun<T> Any.context(function: Context.() -> T) = (when (this) {
    is ComponentActivity -> {
        this
    }
    is Fragment -> {
        requireContext()
    }
    is ListenableWorker -> {
        applicationContext
    }
    else -> throw java.lang.Exception("context is null")
}).run(function)