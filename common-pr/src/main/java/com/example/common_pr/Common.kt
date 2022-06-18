package com.example.common_pr

import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

context(LifecycleOwner)
fun <T> LiveData<T>.withState(ob: Observer<in T>) {
    val owner : LifecycleOwner = this@LifecycleOwner
    val any = if (owner is Fragment) owner.viewLifecycleOwner else owner
    observe(any, ob)
}