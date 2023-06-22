package com.storyteller_f.file_system.model

import android.net.Uri

class DirectoryItemModel(name: String, uri: Uri, isHidden: Boolean, lastModifiedTime: Long, isSymLink: Boolean) :
    FileSystemItemModel(name, uri, isHidden, lastModifiedTime, isSymLink) {
    var fileCount: Long = 0
    var folderCount: Long = 0
}