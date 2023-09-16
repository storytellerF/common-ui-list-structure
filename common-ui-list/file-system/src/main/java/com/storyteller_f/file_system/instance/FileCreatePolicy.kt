package com.storyteller_f.file_system.instance

import androidx.documentfile.provider.DocumentFile

sealed interface FileCreatePolicy {
    object NotCreate : FileCreatePolicy

    class Create(val isFile: Boolean) : FileCreatePolicy
}

sealed interface GetDocumentFile {
    class Failed(val throwable: Throwable) : GetDocumentFile

    class Success(val file: DocumentFile) : GetDocumentFile
}
