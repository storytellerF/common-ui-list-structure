package com.storyteller_f.file_system.util

import android.net.Uri
import com.storyteller_f.file_system.model.DirectoryItemModel
import com.storyteller_f.file_system.model.FileItemModel
import com.storyteller_f.file_system.model.FileSystemItemModel
import com.storyteller_f.file_system.model.TorrentFileItemModel
import java.io.File

object FileInstanceUtility {
    /**
     * 添加普通文件，判断过滤监听事件
     *
     * @param files            填充目的地
     * @param uri              绝对路径
     * @param name             文件名
     * @param isHidden           是否是隐藏文件
     * @param lastModifiedTime 上次访问时间
     * @param extension        文件后缀名
     * @return 返回添加的文件
     */
    private fun addFile(
        files: MutableCollection<FileItemModel>,
        uri: Uri?,
        name: String,
        isHidden: Boolean,
        lastModifiedTime: Long,
        extension: String?,
        permission: String?,
        size: Long
    ): FileItemModel? {
        val fileItemModel: FileItemModel
        fileItemModel = if ("torrent" == extension) {
            TorrentFileItemModel(name, uri!!, isHidden, lastModifiedTime, false)
        } else {
            FileItemModel(name, uri!!, isHidden, lastModifiedTime, false, extension)
        }
        fileItemModel.permissions = permission
        fileItemModel.size = size
        return if (files.add(fileItemModel)) fileItemModel else null
    }

    /**
     * 添加普通目录，判断过滤监听事件
     *
     * @param directories      填充目的地
     * @param uri              绝对路径
     * @param directoryName    文件夹名
     * @param isHidden     是否是隐藏文件
     * @param lastModifiedTime 上次访问时间
     * @return 如果客户端不允许添加，返回null
     */
    private fun addDirectory(
        directories: MutableCollection<DirectoryItemModel>,
        uri: Uri?,
        directoryName: String,
        isHidden: Boolean,
        lastModifiedTime: Long,
        permissions: String?
    ): FileSystemItemModel? {
        val e = DirectoryItemModel(directoryName, uri!!, isHidden, lastModifiedTime, false)
        e.permissions = permissions
        return if (directories.add(e)) e else null
    }

    /**
     * 添加普通目录，判断过滤监听事件
     */
    fun addDirectory(directories: MutableCollection<DirectoryItemModel>, uriPair: Pair<File?, Uri?>?, permissions: String?): FileSystemItemModel? {
        val childDirectory = uriPair!!.first
        val hidden = childDirectory!!.isHidden
        val name = childDirectory.name
        val lastModifiedTime = childDirectory.lastModified()
        return addDirectory(directories, uriPair.second, name, hidden, lastModifiedTime, permissions)
    }

    /**
     * 添加普通目录，判断过滤监听事件
     */
    fun addFile(directories: MutableCollection<FileItemModel>, uriPair: Pair<File?, Uri?>?, permissions: String?): FileSystemItemModel? {
        val childFile = uriPair!!.first
        val hidden = childFile!!.isHidden
        val name = childFile.name
        val lastModifiedTime = childFile.lastModified()
        val extension = FileUtility.getExtension(name)
        val length = childFile.length()
        return addFile(directories, uriPair.second, name, hidden, lastModifiedTime, extension, permissions, length)
    }
}
