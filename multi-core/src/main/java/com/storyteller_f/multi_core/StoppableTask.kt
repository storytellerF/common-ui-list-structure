package com.storyteller_f.multi_core

interface StoppableTask {
    fun needStop(): Boolean
}