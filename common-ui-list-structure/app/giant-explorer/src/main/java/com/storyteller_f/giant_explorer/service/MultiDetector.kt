package com.storyteller_f.giant_explorer.service

import android.net.Uri
import java.util.*

class MultiDetector(private val selected: List<Uri>) {
    fun start(): LinkedList<DetectedTask> {
        val detectedTasks = LinkedList<DetectedTask>()
        for (uri in selected) {
            val scheme = try {
                uri.scheme
            } catch (e: Exception) {
                continue
            }
            when (scheme) {
                "file" -> detectedTasks.add(LocalTask(uri.path!!))
                "http", "https" -> detectedTasks.add(DownloadTask(uri.path!!))
                "content" -> detectedTasks.add(ContentTask(uri))
                else -> detectedTasks.add(ErrorTask(uri.toString() + "无法识别"))
            }
        }
        return detectedTasks
    }
}