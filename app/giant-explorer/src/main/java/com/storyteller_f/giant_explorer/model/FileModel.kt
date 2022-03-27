package com.storyteller_f.giant_explorer.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.storyteller_f.ui_list.core.Model

data class FileModel(
    val name: String,
    val fullPath: String,
    /**
     * -1 代表当前大小未知
     */
    val size: Long,
    val isHidden: Boolean
) : Model {
    override fun commonDatumId() = fullPath
}

