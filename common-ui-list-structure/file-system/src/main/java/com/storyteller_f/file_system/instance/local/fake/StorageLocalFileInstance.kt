package com.storyteller_f.file_system.instance.local.fake

import android.content.Context
import android.os.Build
import com.storyteller_f.file_system.FileInstanceFactory
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.FilesAndDirectories
import com.storyteller_f.file_system.util.FileUtility
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * 标识特殊目录/storage/
 */
class StorageLocalFileInstance(val context: Context) :
    ForbidChangeDirectoryLocalFileInstance(FileInstanceFactory.storagePath) {

    override fun getDirectory(): DirectoryItemModel =
        DirectoryItemModel("storage", path, isHidden, File(FileInstanceFactory.storagePath).lastModified())

    override fun getFileInputStream(): FileInputStream {
        TODO("Not yet implemented")
    }

    override fun getFileOutputStream(): FileOutputStream {
        TODO("Not yet implemented")
    }

    override fun getFileLength(): Long = -1L


    override fun list(
        fileItems: MutableList<FileItemModel>,
        directoryItems: MutableList<DirectoryItemModel>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val storageVolume = FileUtility.getStorageVolume(context)
            storageVolume.mapNotNull {
                it.uuid?.let { uuid ->
                    val s = "${FileInstanceFactory.storagePath}/${uuid}"
                    DirectoryItemModel(uuid, s, false, File(s).lastModified())
                }
            }.toMutableList().apply {
                val path = FileInstanceFactory.emulatedRootPath
                add(DirectoryItemModel("emulated", path, false, File(path).lastModified()))
            }.forEach(directoryItems::add)

        } else {
            FilesAndDirectories.empty()
        }
    }

    override fun exists() = true

    override fun toParent(): FileInstance {
        TODO("Not yet implemented")
    }

    override fun changeToParent() {
        TODO("Not yet implemented")
    }

    override fun getDirectorySize() = -1L

    override fun isHidden() = false

    override fun toChild(name: String, isFile: Boolean, createWhenNotExists: Boolean): FileInstance {
        TODO("Not yet implemented")
    }

    override fun changeToChild(name: String, isFile: Boolean, createWhenNotExists: Boolean) {
        TODO("Not yet implemented")
    }

    override fun changeTo(path: String) {
        TODO("Not yet implemented")
    }

    override fun getParent() = "/"
}