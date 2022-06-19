package com.example.common_pr

import android.util.TypedValue
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.storyteller_f.common_ktx.context

context(LifecycleOwner)
fun <T> LiveData<T>.withState(ob: Observer<in T>) {
    val owner: LifecycleOwner = this@LifecycleOwner
    val any = if (owner is Fragment) owner.viewLifecycleOwner else owner
    observe(any, ob)
}

context(LifecycleOwner)
val Int.dip
    get() = toFloat().dip

context(LifecycleOwner)
val Float.dip: Float
    get() {
        return context {
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this@dip, resources.displayMetrics)
        }
    }

context(LifecycleOwner)
val Int.dipToInt
    get() = toFloat().dipToInt

context(LifecycleOwner)
val Float.dipToInt: Float
    get() {
        return context {
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this@dipToInt, resources.displayMetrics)
        }
    }
