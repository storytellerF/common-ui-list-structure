package com.storyteller_f.file_system_ktx

import android.widget.ImageView
import com.storyteller_f.file_system.R
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.FileSystemItemModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

val FileSystemItemModel.isFile
    get() = this is FileItemModel

val FileSystemItemModel.isDirectory get() = this is DirectoryItemModel

fun ImageView.fileIcon(fileSystemItemModel: FileSystemItemModel) {
    if (fileSystemItemModel is FileItemModel) {
        if (fileSystemItemModel.fullPath.startsWith("/data/app/")) {
            setImageDrawable(context.packageManager.getApplicationIcon(fileSystemItemModel.name))
            return
        }
        val extension = fileSystemItemModel.extension
        if (extension != null) {
            val placeholder = when (extension) {
                "mp3", "wav", "flac" -> R.drawable.ic_music
                "zip", "7z", "rar" -> R.drawable.ic_archive
                "jpg", "jpeg", "png", "gif" -> R.drawable.ic_image
                "mp4", "rmvb", "ts", "avi", "mkv", "3gp", "mov", "flv", "m4s" -> R.drawable.ic_video
                "url" -> R.drawable.ic_url
                "txt", "c" -> R.drawable.ic_text
                "js" -> R.drawable.ic_js
                "pdf" -> R.drawable.ic_pdf
                "doc", "docx" -> R.drawable.ic_word
                "xls", "xlsx" -> R.drawable.ic_excel
                "ppt", "pptx" -> R.drawable.ic_ppt
                "iso" -> R.drawable.ic_disk
                "exe", "msi" -> R.drawable.ic_exe
                "psd" -> R.drawable.ic_psd
                "torrent" -> R.drawable.ic_torrent
                else -> R.drawable.ic_unknow
            }
            setImageResource(placeholder)
        } else {
            setImageResource(R.drawable.ic_binary)
        }
    } else setImageResource(R.drawable.ic_folder_explorer)
}

suspend fun File.ensureFile(): File? {
    if (!exists()) {
        parentFile?.ensureDirs() ?: return null
        if (!withContext(Dispatchers.IO) {
                createNewFile()
            }) {
            return null
        }
    }
    return this
}

suspend fun File.ensureDirs(): File? {
    if (!exists()) {
        if (!withContext(Dispatchers.IO) {
                mkdirs()
            }) {
            return null
        }
    }
    return this
}