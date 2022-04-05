package com.storyteller_f.giant_explorer.service

import com.storyteller_f.file_system.model.FileSystemItemModel

open class DetectorTask internal constructor(val file: FileSystemItemModel)

class ErrorTask(path: FileSystemItemModel?, var message: String) : DetectorTask(path!!)

class ValidTask(path: FileSystemItemModel?, val type: Int) : DetectorTask(path!!) {

    companion object {
        const val type_file = 1
        const val type_empty = 2
        const val type_not_empty = 3
    }
}