package com.storyteller_f.giant_explorer.model

import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.ui_list.core.Model

data class FileModel(
    val name: String,
    val fullPath: String,
    /**
     * -1 代表当前大小未知
     */
    val size: Long,
    val isHidden: Boolean,
    val item: FileSystemItemModel,
    val isSymLink: Boolean,
) : Model {
    override fun commonId() = fullPath
}

