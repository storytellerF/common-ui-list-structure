package com.storyteller_f.file_system.message

import java.util.*

class Commission(private val commissionName: String) {
    val messages: LinkedList<Message>
    fun setSuccess(success: Boolean): Boolean {
        isSuccess = success
        return success
    }

    var isSuccess: Boolean
        private set

    fun addMessage(message: Message) {
        messages.add(message)
    }

    fun addCommission(commission: Commission): Boolean {
        messages.addAll(commission.messages)
        return setSuccess(commission.isSuccess)
    }

    val messageIterator: Iterator<Message>
        get() = messages.iterator()

    fun addToLast(m: String?) {
        val last = messages.last
        last.add(m)
    }

    fun print() {
        val iterator = messageIterator
        println(messages.size)
        println("会话$commissionName")
        while (iterator.hasNext()) {
            val next = iterator.next()
            println(
                """	${next.name}
		${next.get()}"""
            )
        }
    }

    val last: Message
        get() = messages.last

    init {
        messages = LinkedList()
        isSuccess = true
    }
}