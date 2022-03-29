package com.storyteller_f.file_system.instance.local.fake

import android.content.Context
import android.os.Build
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.local.ForbidChangeDirectoryLocalFileInstance
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FilesAndDirectories
import com.storyteller_f.file_system.util.FileUtility
import java.io.File

class StorageLocalFileInstance(val context: Context) :
    ForbidChangeDirectoryLocalFileInstance("/storage") {

    override fun getDirectory(): DirectoryItemModel =
        DirectoryItemModel("storage", path, isHide, File("/storage").lastModified())

    override fun getFileLength(): Long = -1L


    override fun list(): FilesAndDirectories {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val storageVolume = FileUtility.getStorageVolume(context)
            FilesAndDirectories(
                mutableListOf(),
                storageVolume.mapNotNull {
                    it.uuid?.let { uuid ->
                        DirectoryItemModel(uuid, "/storage/${uuid}", false, 0)
                    }
                }.toMutableList().apply {
                    add(DirectoryItemModel("emulated", "/storage/emulated", false, 0))
                }
            )
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

    override fun isHide() = false

    override fun toChild(name: String?, isFile: Boolean, reCreate: Boolean): FileInstance {
        TODO("Not yet implemented")
    }

    override fun changeToChild(name: String?, isFile: Boolean, reCreate: Boolean) {
        TODO("Not yet implemented")
    }

    override fun changeTo(path: String?) {
        TODO("Not yet implemented")
    }

    override fun getParent() = "/"
    override fun listSafe() = list()
}