package com.storyteller_f.common_vm_ktx

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.storyteller_f.ext_func_definition.ExtFuncFlat
import com.storyteller_f.ext_func_definition.ExtFuncFlatType

/**
 * @author storyteller_f
 */

@ExtFuncFlat(ExtFuncFlatType.V7)
data class Dao1<out D1>(val d1: D1)

@ExtFuncFlat(ExtFuncFlatType.V6)
fun <T1> combineDao(s1: LiveData<T1?>): MediatorLiveData<Dao1<T1?>> {
    val mediatorLiveData = MediatorLiveData<Dao1<T1?>>()
    mediatorLiveData.addSource(s1) {
        mediatorLiveData.value = Dao1(it)
    }
    return mediatorLiveData
}

infix fun <A, B, C> Pair<A, B>.to(that: C) = Triple(first, second, that)

infix fun <A, B, C> Pair<A, B>.plus(that: Pair<A, C>) = mutableListOf(this, that)

infix fun <A, B> MutableList<Pair<A, Any?>>.plus(that: Pair<A, B>): List<Pair<A, Any?>> =
    apply {
        add(that)
    }
