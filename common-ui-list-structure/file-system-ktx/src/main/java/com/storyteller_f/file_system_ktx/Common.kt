package com.storyteller_f.file_system_ktx

import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.FileSystemItemModel

val FileSystemItemModel.isFile
    get() = this is FileItemModel

val FileSystemItemModel.isDirectory get() = this is DirectoryItemModel