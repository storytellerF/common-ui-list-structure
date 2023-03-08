package com.storyteller_f.file_system.model

class DirectoryItemModel(name: String, path: String, isHidden: Boolean, lastModifiedTime: Long, isSymLink: Boolean) :
    FileSystemItemModel(name, path, isHidden, lastModifiedTime, isSymLink) {
    var fileCount: Long = 0
    var folderCount: Long = 0
}