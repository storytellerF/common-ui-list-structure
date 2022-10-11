package com.storyteller_f.file_system.model

class SimpleFileItemModel(name: String?, absolutePath: String?, var length: Long) :
    FileSystemItemModelLite(
        name!!, absolutePath!!
    )