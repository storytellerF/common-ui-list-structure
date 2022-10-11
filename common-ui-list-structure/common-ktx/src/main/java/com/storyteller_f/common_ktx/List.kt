package com.storyteller_f.common_ktx

fun <T> MutableList<T>.toggle(t: T) {
    if (contains(t)) {
        remove(t)
    } else add(t)
}

/**
 * 省略一个问号，在数值不为null时，也用去去除多余的问号
 */
fun <T, R> T?.mm(block: (T) -> R): R? {
    return this?.let { block(it) }
}