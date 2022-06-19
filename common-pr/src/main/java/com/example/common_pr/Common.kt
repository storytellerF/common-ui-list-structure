package com.example.common_pr

import android.content.Context
import android.util.TypedValue
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.example.ext_func_definition.ExtFuncFlat

context(LifecycleOwner)
fun <T> LiveData<T>.withState(ob: Observer<in T>) {
    val owner: LifecycleOwner = this@LifecycleOwner
    val any = if (owner is Fragment) owner.viewLifecycleOwner else owner
    observe(any, ob)
}

context(Context)
@ExtFuncFlat
val Int.dip
    get() = toFloat().dip

context(Context)
@ExtFuncFlat
val Float.dip: Float
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this@dip, resources.displayMetrics)

context(Context)
@ExtFuncFlat
val Int.dipToInt
    get() = toFloat().dipToInt

context(Context)
@ExtFuncFlat
val Float.dipToInt: Float
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this@dipToInt, resources.displayMetrics)
