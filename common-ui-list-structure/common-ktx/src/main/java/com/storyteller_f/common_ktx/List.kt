package com.storyteller_f.common_ktx

fun <T> MutableList<T>.toggle(t: T) {
    if (contains(t)) {
        remove(t)
    } else add(t)
}

/**
 * 省略一个问号，在数值不为null时，也用去去除多余的问号
 */
fun <T, R> T?.nn(block: (T) -> R): R? {
    return this?.let { block(it) }
}

fun <T> List<T>.same(list: List<T>): Boolean {
    if (size != list.size) return false
    forEachIndexed { index, t ->
        if (list[index] != t) {
            return false
        }
    }
    return true
}
