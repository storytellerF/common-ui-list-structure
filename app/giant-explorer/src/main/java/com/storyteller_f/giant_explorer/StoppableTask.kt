package com.storyteller_f.giant_explorer

import androidx.work.ListenableWorker
import com.storyteller_f.multi_core.StoppableTask


class WorkerStoppableTask(private val worker: ListenableWorker) : StoppableTask {
    override fun needStop() = worker.isStopped
}