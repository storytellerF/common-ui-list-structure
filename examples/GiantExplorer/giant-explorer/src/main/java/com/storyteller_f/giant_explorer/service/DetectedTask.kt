package com.storyteller_f.giant_explorer.service

import android.net.Uri

abstract class DetectedTask

class ErrorTask(var message: String) : DetectedTask()

class LocalTask(val path: String) : DetectedTask()

class ContentTask(val uri: Uri) : DetectedTask()

class DownloadTask(val url: String) : DetectedTask()