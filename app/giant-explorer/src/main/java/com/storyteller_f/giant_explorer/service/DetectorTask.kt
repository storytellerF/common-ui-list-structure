package com.storyteller_f.giant_explorer.service

import android.net.Uri

abstract class DetectorTask

class ErrorTask(var message: String) : DetectorTask()

class LocalTask(val path: String) : DetectorTask()

class ContentTask(val uri: Uri) : DetectorTask()

class DownloadTask(val url: String) : DetectorTask()