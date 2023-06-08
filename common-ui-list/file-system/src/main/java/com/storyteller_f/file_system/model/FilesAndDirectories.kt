package com.storyteller_f.file_system.model

class FilesAndDirectories(
    val files: MutableList<FileItemModel>,
    val directories: MutableList<DirectoryItemModel>
) {

    fun addFiles(fileItemModels: List<FileItemModel>?) {
        files.addAll(fileItemModels!!)
    }

    fun addDirectory(directoryItemModels: List<DirectoryItemModel>?) {
        directories.addAll(directoryItemModels!!)
    }

    fun destroy() {
        files.clear()
        directories.clear()
    }

    val count: Int
        get() = files.size + directories.size

    companion object {
        fun empty() = FilesAndDirectories(mutableListOf(), mutableListOf())
    }
}