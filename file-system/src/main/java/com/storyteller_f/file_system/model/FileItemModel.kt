package com.storyteller_f.file_system.model

import java.io.File

open class FileItemModel : FileSystemItemModel {
    var md: String? = null
    val extension: String

    constructor(
        file: File,
        isHide: Boolean,
        lastModifiedTime: Long,
        extension: String
    ) : super(file.name, file.absolutePath, isHide, lastModifiedTime) {
        this.extension = extension
    }

    constructor(
        name: String,
        absolutePath: String?,
        isHide: Boolean,
        lastModifiedTime: Long,
        extension: String
    ) : super(
        name, absolutePath!!, isHide, lastModifiedTime
    ) {
        this.extension = extension
    }
}