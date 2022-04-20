package com.storyteller_f.file_system.model

import java.io.File

open class FileItemModel : FileSystemItemModel {
    var md: String? = null

    /**
     * first char is not [.]
     */
    val extension: String?

    constructor(
        file: File,
        isHide: Boolean,
        extension: String? = null
    ) : super(file.name, file.absolutePath, isHide, file.lastModified()) {
        this.extension = extension
    }

    constructor(
        name: String,
        fullPath: String,
        isHidden: Boolean,
        lastModifiedTime: Long,
        extension: String? = null
    ) : super(
        name, fullPath, isHidden, lastModifiedTime
    ) {
        this.extension = extension
    }
}