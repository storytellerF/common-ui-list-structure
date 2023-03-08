package com.storyteller_f.file_system.model

import java.io.File

open class FileItemModel : FileSystemItemModel {
    var md: String? = null

    /**
     *  A file extension without the leading '.'
     */
    val extension: String?

    constructor(
        file: File,
        isHide: Boolean = file.isHidden,
        extension: String = file.extension,
        isSymLink: Boolean,
    ) : super(file.name, file.absolutePath, isHide, file.lastModified(), isSymLink) {
        this.extension = extension
    }

    constructor(
        name: String,
        fullPath: String,
        isHidden: Boolean,
        lastModifiedTime: Long,
        extension: String? = null,
        isSymLink: Boolean,
    ) : super(
        name, fullPath, isHidden, lastModifiedTime, isSymLink
    ) {
        this.extension = extension
    }
}