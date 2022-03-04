@file:Suppress("UNCHECKED_CAST", "unused")

package com.storyteller_f.common_vm_ktx

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import kotlin.collections.set

/**
 * @author storyteller_f
 */

class CountableMediatorLiveData : MediatorLiveData<Any>() {
    var currentIndex = 0
}

class KeyedLiveData<T>(value: T, val key: String = "") : MutableLiveData<T>(value)

fun LiveData<out Any>.plus(source: LiveData<out Any>, key: String = ""): LiveData<out Any> {
    val sourceKey = if (source is KeyedLiveData<*> && source.key.trim().isNotEmpty()) {
        source.key
    } else key
    if (this is CountableMediatorLiveData) {
        val index = currentIndex
        val k = if (sourceKey.trim().isEmpty()) index.toString() else key
        addSource(source) {
            value = copyMap(value as Map<String, Any?>?).apply {
                set(k, it)
            }
        }
        currentIndex++
        return this
    } else {
        val mediatorLiveData = CountableMediatorLiveData()
        val k1 = if (this is KeyedLiveData<*> && this.key.trim().isNotEmpty()) {
            this.key
        } else {
            "first"
        }
        mediatorLiveData.addSource(this) {
            if (mediatorLiveData.value is Map<*, *>) {
                mediatorLiveData.value =
                    copyMap(mediatorLiveData.value as Map<String, Any?>?).apply {
                        set(k1, it)
                    }
            }
        }
        val k = if (sourceKey.trim().isEmpty()) "second" else sourceKey

        mediatorLiveData.addSource(source) {
            mediatorLiveData.value =
                copyMap(mediatorLiveData.value as Map<String, Any?>?).apply {
                    set(k, it)
                }
        }
        mediatorLiveData.currentIndex = 2
        return mediatorLiveData
    }
}

fun combine(vararg arrayOfPairs: Pair<String, LiveData<out Any?>>): LiveData<Map<String, Any?>> {
    val mediatorLiveData = MediatorLiveData<Map<String, Any?>>()
    arrayOfPairs.forEach {
        val index = it.first
        mediatorLiveData.addSource(it.second) {
            mediatorLiveData.value = copyMap(mediatorLiveData.value).apply {
                set(index, it)
            }
        }
    }
    return mediatorLiveData.combine()
}

fun <X> LiveData<X>.combine(): LiveData<Map<String, Any?>> =
    Transformations.map(this) { it as Map<String, Any?> }

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


fun List<Any?>.gon(index: Int): Any? {
    return getOrNull(index)
}

fun copyList(list: List<Any?>?): MutableList<Any?> {
    val newly = mutableListOf<Any?>()
    list?.forEach {
        newly.add(it)
    }
    return newly
}

fun copyMap(map: Map<String, Any?>?): MutableMap<String, Any?> {
    val newly = mutableMapOf<String, Any?>()
    map?.forEach {
        newly[it.key] = it.value
    }
    return newly
}
