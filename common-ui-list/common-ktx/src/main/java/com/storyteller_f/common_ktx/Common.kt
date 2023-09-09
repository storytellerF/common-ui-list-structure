package com.storyteller_f.common_ktx

fun Int.bit(bit: Int) = this and bit == bit

val Throwable.exceptionMessage get() = localizedMessage ?: javaClass.toString()
