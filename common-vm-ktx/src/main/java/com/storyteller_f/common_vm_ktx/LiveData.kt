package com.storyteller_f.common_vm_ktx

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

/**
 * @author storyteller_f
 */

class CountableMediatorLiveData : MediatorLiveData<Any>() {
    var currentIndex = 0
}

fun LiveData<out Any>.plus(source: LiveData<out Any>): LiveData<out Any> {
    if (this is CountableMediatorLiveData) {
        val index = currentIndex
        addSource(source) {
            value = copyList(value as List<Any?>?).apply {
                addOrSet(index, it)
            }
        }
        currentIndex++
        return this
    } else {
        val mediatorLiveData = CountableMediatorLiveData()
        mediatorLiveData.addSource(this) {
            mediatorLiveData.value = copyList(mediatorLiveData.value as List<Any?>?).apply {
                addOrSet(0, it)
            }
        }
        mediatorLiveData.addSource(source) {
            mediatorLiveData.value = copyList(mediatorLiveData.value as List<Any?>?).apply {
                addOrSet(1, it)
            }
        }
        mediatorLiveData.currentIndex = 2
        return mediatorLiveData
    }
}

private fun <E> MutableList<E?>.addOrSet(e: Int, it: E) {
    synchronized(this) {
        if (size <= e) {
            //追加到指定位置
            for (i in size..e) {
                add(null)
            }
        }
    }
    set(e, it)
}

fun List<Any?>.gor(index: Int): Any? {
    return when {
        index < size -> {
            get(index)
        }
        else -> null
    }
}

fun copyList(list: List<Any?>?): MutableList<Any?> {
    val newly = mutableListOf<Any?>()
    list?.forEach {
        newly.add(it)
    }
    return newly
}
