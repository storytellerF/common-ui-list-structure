package com.storyteller_f.giant_explorer.service

import android.net.Uri
import com.storyteller_f.giant_explorer.service.DetectorTask
import com.storyteller_f.giant_explorer.service.LocalTask
import com.storyteller_f.giant_explorer.service.DownloadTask
import com.storyteller_f.giant_explorer.service.ContentTask
import com.storyteller_f.giant_explorer.service.ErrorTask
import java.lang.Exception
import java.util.*

class MultiDetector(private val selected: List<Uri>) {
    fun start(): LinkedList<DetectorTask> {
        val detectorTasks = LinkedList<DetectorTask>()
        for (uri in selected) {
            val scheme = try {
                uri.scheme
            } catch (e: Exception) {
                continue
            }
            when (scheme) {
                "file" -> detectorTasks.add(LocalTask(uri.path!!))
                "http", "https" -> detectorTasks.add(DownloadTask(uri.path!!))
                "content" -> detectorTasks.add(ContentTask(uri))
                else -> detectorTasks.add(ErrorTask(uri.toString() + "无法识别"))
            }
        }
        return detectorTasks
    }
}