package com.storyteller_f.common_vm_ktx

fun <E> MutableList<E?>.addOrSet(e: Int, it: E) {
    synchronized(this) {
        if (size <= e) {
            // 追加到指定位置
            repeat(e - size + 1) {
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
