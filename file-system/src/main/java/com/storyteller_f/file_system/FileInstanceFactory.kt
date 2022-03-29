package com.storyteller_f.file_system

import android.content.Context
import android.os.Build
import android.os.StatFs
import android.util.Log
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.instance.FileInstance
import com.storyteller_f.file_system.instance.local.document.ExternalDocumentLocalFileInstance
import com.storyteller_f.file_system.instance.local.RegularLocalFileInstance
import com.storyteller_f.file_system.instance.local.fake.EmulatedLocalFileInstance
import com.storyteller_f.file_system.instance.local.fake.StorageLocalFileInstance
import com.storyteller_f.file_system.instance.local.document.MountedLocalFileInstance
import com.storyteller_f.file_system.instance.local.fake.FakeDirectoryLocalFileInstance
import java.lang.Exception
import java.util.ArrayList

object FileInstanceFactory {
    private const val TAG = "FileInstanceFactory"
    const val rootUserEmulatedPath = "/storage/emulated/0"
    const val emulatedRootPath = "/storage/emulated"
    const val currentEmulatedPath = "/storage/self"
    const val storagePath = "/storage"
    const val sdCardPath = "/sdcard"
    private val filter: Filter = object : Filter {
        override fun onPath(parent: String, absolutePath: String, isFile: Boolean): Boolean {
            return true
        }

        override fun onFile(parent: String): List<FileItemModel> {
            return ArrayList()
        }

        override fun onDirectory(parent: String): List<DirectoryItemModel> {
            return ArrayList()
        }
    }

    @JvmStatic
    fun getFileInstance(path: String, context: Context): FileInstance {
        return getFileInstance(filter, path, context)
    }

    private fun getFileInstance(filter: Filter?, path: String, context: Context): FileInstance {
        assert(!path.endsWith("/"))
        return when {
            path.startsWith(rootUserEmulatedPath) -> if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R && Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                ExternalDocumentLocalFileInstance(filter, context, path)
            } else {
                RegularLocalFileInstance(context, filter, path)
            }
            path.startsWith(emulatedRootPath) -> EmulatedLocalFileInstance(context, filter)
            path.startsWith(currentEmulatedPath) -> RegularLocalFileInstance(context, filter, path)
            path == storagePath -> StorageLocalFileInstance(context)
            path.startsWith(storagePath) -> when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                    RegularLocalFileInstance(context, filter, path)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ->
                    MountedLocalFileInstance(filter, context, path)
                Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1 -> {
                    Log.e(
                        TAG,
                        "getFileInstance: 当前版本不支持formTreeUri，测试性的返回"
                    )
                    RegularLocalFileInstance(context, filter, path)
                }
                else -> RegularLocalFileInstance(context, filter, path)
            }
            path.startsWith(sdCardPath) -> RegularLocalFileInstance(context, filter, path)
            path.startsWith("/mnt") -> RegularLocalFileInstance(context, filter, path)
            path.startsWith("/data/data/${context.packageName}") -> RegularLocalFileInstance(context, filter, path)
            else -> FakeDirectoryLocalFileInstance(path, context)
        }
    }

    @JvmStatic
    fun getPrefix(path: String, context: Context): String {
        assert(!path.endsWith("/")) {
            path
        }
        return when {
            path.startsWith(rootUserEmulatedPath) -> rootUserEmulatedPath
            path.startsWith(emulatedRootPath) -> emulatedRootPath
            path == storagePath -> "/storage"
            path.startsWith(storagePath) -> {
                var endIndex = path.indexOf("/", storagePath.length)
                if (endIndex == -1) endIndex = path.length
                path.substring(0, endIndex + 1)
            }
            path.startsWith(sdCardPath) -> sdCardPath
            path.startsWith("/mnt") -> "/mnt"
            path.startsWith("/data/data/${context.packageName}") -> "/data/data/${context.packageName}"
            else -> ""
        }
    }

    fun getSpace(prefix: String?): Long {
        val stat = StatFs(prefix)
        val blockSize: Long
        val availableBlocks: Long
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            blockSize = stat.blockSizeLong
            availableBlocks = stat.availableBlocksLong
        } else {
            blockSize = stat.blockSize.toLong()
            availableBlocks = stat.availableBlocks.toLong()
        }
        return blockSize * availableBlocks
    }

    @Throws(Exception::class)
    fun toChild(
        fileInstance: FileInstance,
        name: String,
        isFile: Boolean,
        context: Context
    ): FileInstance {
        val parentPrefix = getPrefix(fileInstance.path, context)
        val path = fileInstance.path + "/" + name
        val childPrefix = getPrefix(path, context)
        return if (parentPrefix == childPrefix) {
            fileInstance.toChild(name, isFile, false)
        } else {
            getFileInstance(path, context)
        }
    }

    @Throws(Exception::class)
    fun toParent(fileInstance: FileInstance, context: Context): FileInstance {
        val parentPrefix = getPrefix(fileInstance.parent, context)
        val childPrefix = getPrefix(fileInstance.path, context)
        return if (parentPrefix == childPrefix) {
            fileInstance.toParent()
        } else {
            getFileInstance(fileInstance.parent, context)
        }
    }
}