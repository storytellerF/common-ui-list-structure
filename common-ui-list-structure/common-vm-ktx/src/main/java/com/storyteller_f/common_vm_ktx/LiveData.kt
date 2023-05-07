@file:Suppress("UNCHECKED_CAST", "unused")

package com.storyteller_f.common_vm_ktx

import android.util.Log
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.storyteller_f.ext_func_definition.ExtFuncFlat
import com.storyteller_f.ext_func_definition.ExtFuncFlatType
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.set

/**
 * @author storyteller_f
 */

class CountableMediatorLiveData : MediatorLiveData<Any>() {
    var currentIndex = 0
}

class KeyedLiveData<T>(value: T, val key: String = "") : MutableLiveData<T>(value)

/**
 * 合并LiveData，value 变成map
 */
fun LiveData<out Any>.plus(source: LiveData<out Any>, key: String = ""): MediatorLiveData<out Any> {
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

fun <T> copyList(list: List<T?>?): MutableList<T?> {
    val newly = mutableListOf<T?>()
    list?.forEach {
        newly.add(it)
    }
    return newly
}


fun <T> copyListNotNull(list: List<T?>?): MutableList<T> {
    val newly = mutableListOf<T>()
    list?.forEach {
        it?.let { it1 -> newly.add(it1) }
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

/**
 * 控制能否让下游observe 到数据变动
 */
class MuteLiveEvent<T> : MutableLiveData<T?>() {
    private val mPending: AtomicBoolean = AtomicBoolean(false)

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T?>) {
        if (hasActiveObservers()) {
            Log.w(TAG, "Multiple observers registered but only one will be notified of changes.")
        }

        // Observe the internal MutableLiveData
        super.observe(owner) {
            if (mPending.get()) {
                observer.onChanged(it)
            }
        }
    }

    @MainThread
    override fun setValue(t: T?) {
        mPending.set(true)
        super.setValue(t)
    }

    /**
     * Used for cases where T is Void, to make calls cleaner.
     */
    @MainThread
    fun call() {
        value = null
    }

    fun reset(t: T?) {
        mPending.set(false)
        super.setValue(t)
    }

    companion object {
        private const val TAG = "SingleLiveEvent"
    }
}

class SingleLiveEvent<T> : MutableLiveData<T?>() {
    private val mPending: AtomicBoolean = AtomicBoolean(false)

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in T?>) {
        if (hasActiveObservers()) {
            Log.w(TAG, "Multiple observers registered but only one will be notified of changes.")
        }

        // Observe the internal MutableLiveData
        super.observe(owner) {
            if (mPending.compareAndSet(true, false)) {
                observer.onChanged(it)
            }
        }
    }

    @MainThread
    override fun setValue(t: T?) {
        mPending.set(true)
        super.setValue(t)
    }

    /**
     * Used for cases where T is Void, to make calls cleaner.
     */
    @MainThread
    fun call() {
        value = null
    }

    companion object {
        private const val TAG = "SingleLiveEvent"
    }
}


fun <T> LiveData<T>.toDiff(compare: ((T, T) -> Boolean)? = null): MediatorLiveData<Pair<T?, T?>> {
    val mediatorLiveData = MediatorLiveData<Pair<T?, T?>>()
    var oo: T? = value
    mediatorLiveData.addSource(this) {
        val l = oo
        if (l == null || it == null || compare?.invoke(l, it) != true) {
            mediatorLiveData.value = Pair(l, it)
        }
        oo = it
    }
    return mediatorLiveData
}

/**
 * @param compare 如果返回真，那么就会被过滤掉
 */
fun <T> LiveData<T>.toDiffNoNull(compare: ((T, T) -> Boolean)? = null): MediatorLiveData<Pair<T, T>> {
    val mediatorLiveData = MediatorLiveData<Pair<T, T>>()
    var oo: T? = value
    mediatorLiveData.addSource(this) {
        val l = oo
        if (l != null && it != null && compare?.invoke(l, it) != true) {
            mediatorLiveData.value = Pair(l, it)
        }
        oo = it
    }
    return mediatorLiveData
}


fun <T> LiveData<T>.debounce(ms: Long): MediatorLiveData<T> {
    val mediatorLiveData = MediatorLiveData<T>()
    var lastTime: Long? = null
    val timer = Timer()
    mediatorLiveData.addSource(this) {
        val l = lastTime
        if (l == null || l - System.currentTimeMillis() >= ms) {
            timer.purge()
            mediatorLiveData.value = it
            lastTime = System.currentTimeMillis()
        } else {
            timer.schedule(object : TimerTask() {
                override fun run() {
                    mediatorLiveData.postValue(it)
                }
            }, ms)
        }
    }
    return mediatorLiveData
}

fun <T> LiveData<T>.withState(owner: LifecycleOwner, ob: Observer<in T>) {
    val any = if (owner is Fragment) owner.viewLifecycleOwner else owner
    observe(any, ob)
}

@ExtFuncFlat(ExtFuncFlatType.V6)
fun<T1, T2> combine(s1: LiveData<T1?>, s2: LiveData<T2?>): MediatorLiveData<Dao2<T1?, T2?>> {
    val mediatorLiveData = MediatorLiveData<Dao2<T1?, T2?>>()
    var v1 = s1.value
    var v2 = s2.value
    mediatorLiveData.addSource(s2) {
        v2 = it
        mediatorLiveData.value = Dao2(v1, it)
    }
    mediatorLiveData.addSource(s1) {
        v1 = it
        mediatorLiveData.value = Dao2(it, v2)
    }
    return mediatorLiveData
}

/**
 * @param f 返回是否相等
 */
fun <X> LiveData<X>.distinctUntilChangedBy(f: (X, X) -> Boolean): LiveData<X?> {
    val outputLiveData: MediatorLiveData<X?> = MediatorLiveData<X?>()
    outputLiveData.addSource(this, object : Observer<X?> {
        var mFirstTime = true
        var previous: X? = null
        override fun onChanged(value: X?) {
            val previousValue = previous
            if (mFirstTime || previousValue == null && value != null || previousValue != null && (previousValue != value || !f(previousValue, value))) {
                mFirstTime = false
                outputLiveData.value = value
                previous = value
            }
        }
    })
    return outputLiveData
}
