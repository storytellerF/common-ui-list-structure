package com.storyteller_f.file_system.model

class DirectoryItemModel(name: String, absPath: String, isHide: Boolean, lastModifiedTime: Long) :
    FileSystemItemModel(name, absPath, isHide, lastModifiedTime) {
    var fileCount: Long = 0
    var folderCount: Long = 0
}