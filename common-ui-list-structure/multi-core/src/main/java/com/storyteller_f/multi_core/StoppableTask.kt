package com.storyteller_f.multi_core

interface StoppableTask {
    fun needStop(): Boolean

    object Blocking : StoppableTask {
        override fun needStop(): Boolean {
            return false
        }

    }
}