package com.storyteller_f.common_ktx

fun <T> MutableList<T>.toggle(t: T) {
    if (contains(t)) {
        remove(t)
    } else add(t)
}

fun <T, R> T?.mm(block: (T) -> R): R? {
    return this?.let { block(it) }
}