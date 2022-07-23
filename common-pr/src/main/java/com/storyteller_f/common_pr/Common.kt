package com.storyteller_f.common_pr

import android.content.Context
import android.util.TypedValue
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.storyteller_f.ext_func_definition.ExtFuncFlat
import com.storyteller_f.ext_func_definition.ExtFuncFlatType

context(LifecycleOwner)
fun <T> LiveData<T>.withState(ob: Observer<in T>) {
    val owner: LifecycleOwner = this@LifecycleOwner
    val any = if (owner is Fragment) owner.viewLifecycleOwner else owner
    observe(any, ob)
}

context(Context)
@ExtFuncFlat(ExtFuncFlatType.V4, isContextReceiver = true)
val Float.dip: Float
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this@dip, resources.displayMetrics)

context(Context)
@ExtFuncFlat(ExtFuncFlatType.V4, isContextReceiver = true)
val Float.dipToInt: Float
    get() = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this@dipToInt, resources.displayMetrics)