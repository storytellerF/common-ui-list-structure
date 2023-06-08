package com.storyteller_f.file_system.model

class SimpleDirectoryItemModel(name: String?, absolutePath: String?) : FileSystemItemModelLite(
    name!!, absolutePath!!
) {
    var fileCount: Long = 0
    var folderCount: Long = 0
}