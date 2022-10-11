package com.storyteller_f.file_system.model

class DirectoryItemModel(name: String, path: String, isHidden: Boolean, lastModifiedTime: Long) :
    FileSystemItemModel(name, path, isHidden, lastModifiedTime) {
    var fileCount: Long = 0
    var folderCount: Long = 0
}