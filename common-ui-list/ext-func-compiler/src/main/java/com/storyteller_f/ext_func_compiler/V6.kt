package com.storyteller_f.ext_func_compiler

import com.storyteller_f.slim_ktx.yes

internal fun generateForV6(): String {
    return MutableList(8) {
        val count = 2 + it
        val genericList = repeat("T1?", count)
        val type = "MediatorLiveData<Dao$count<$genericList>>"

        combineDao(count, type)
    }.joinToString("\n")
}

private fun combineDao(count: Int, type: String): String {
    return """
        fun<${repeat("T1", count)}> combineDao(${repeat("s1: LiveData<T1>", count)}): $type {
            val mediatorLiveData = $type()
            ${repeat("var d1 = s1.value\n", count, sp = "\n").yes(3).indentRest()}
            ${liveDataAddSource(count).yes(3).indentRest()}
            return mediatorLiveData
        }
        """.trimIndent()
}

private fun liveDataAddSource(count: Int): String {
    return MutableList(count) {
        val current = it + 1
        """
        mediatorLiveData.addSource(s${current}) {
            d${current} = it
            mediatorLiveData.value = Dao$count(${repeat("d1", current - 1, end = ", ")}it, ${repeat("d1", count - current, current + 1)})
        }
        """.trimIndent()
    }.joinToString("\n")
}