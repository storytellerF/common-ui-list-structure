package com.storyteller_f.file_system.model

import android.net.Uri

open class FileSystemItemModelLite(val name: String, val uri: Uri) {
    val fullPath: String = uri.path!!
}
