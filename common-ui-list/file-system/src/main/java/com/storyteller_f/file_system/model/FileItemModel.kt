package com.storyteller_f.file_system.model

import android.net.Uri
import java.io.File

open class FileItemModel : FileSystemItemModel {
    var md: String? = null

    /**
     *  A file extension without the leading '.'
     */
    val extension: String?

    constructor(
        file: File,
        uri: Uri,
        isHide: Boolean = file.isHidden,
        extension: String = file.extension,
        isSymLink: Boolean,
    ) : super(file.name, uri, isHide, file.lastModified(), isSymLink) {
        this.extension = extension
    }

    constructor(
        name: String,
        uri: Uri,
        isHidden: Boolean,
        lastModifiedTime: Long,
        isSymLink: Boolean,
        extension: String? = null,
    ) : super(
        name, uri, isHidden, lastModifiedTime, isSymLink
    ) {
        this.extension = extension
    }
}