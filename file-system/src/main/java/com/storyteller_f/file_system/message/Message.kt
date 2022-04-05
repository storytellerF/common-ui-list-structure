package com.storyteller_f.file_system.message

import java.lang.StringBuilder

class Message(val name: String) {
    val time = System.currentTimeMillis()
    val content = StringBuilder()
    fun add(m: Boolean): Message {
        content.append(m)
        return this
    }

    fun add(message: Int): Message {
        content.append(message)
        return this
    }

    fun add(message: String?): Message {
        content.append(message)
        return this
    }

    fun append(message: String?): Message {
        add(message)
        return this
    }

    fun get(): String {
        return content.toString()
    }

}