package com.storyteller_f.file_system.util;

import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.storyteller_f.file_system.model.DirectoryItemModel;
import com.storyteller_f.file_system.model.FileItemModel;
import com.storyteller_f.file_system.model.FileSystemItemModel;
import com.storyteller_f.file_system.model.TorrentFileItemModel;

import java.io.File;
import java.util.Collection;
import java.util.List;

import kotlin.Pair;

public class FileInstanceUtility {

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
    private static FileItemModel addFile(Collection<FileItemModel> files, Uri uri, String name, boolean isHidden, long lastModifiedTime, String extension, String permission, long size) {
        FileItemModel fileItemModel;
        if ("torrent".equals(extension)) {
            fileItemModel = new TorrentFileItemModel(name, uri, isHidden, lastModifiedTime, false);
        } else {
            fileItemModel = new FileItemModel(name, uri, isHidden, lastModifiedTime, false, extension);
        }
        fileItemModel.setPermissions(permission);
        fileItemModel.setSize(size);
        if (files.add(fileItemModel)) return fileItemModel;
        return null;
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
    private static FileSystemItemModel addDirectory(Collection<DirectoryItemModel> directories, Uri uri, String directoryName, boolean isHidden, long lastModifiedTime, String permissions) {
        DirectoryItemModel e = new DirectoryItemModel(directoryName, uri, isHidden, lastModifiedTime, false);
        e.setPermissions(permissions);
        if (directories.add(e)) return e;
        return null;
    }

    /**
     * 添加普通目录，判断过滤监听事件
     */
    public static FileSystemItemModel addDirectory(Collection<DirectoryItemModel> directories, Pair<File, Uri> uriPair, String permissions) {
        var childDirectory = uriPair.getFirst();
        boolean hidden = childDirectory.isHidden();
        String name = childDirectory.getName();
        long lastModifiedTime = childDirectory.lastModified();
        return addDirectory(directories, uriPair.getSecond(), name, hidden, lastModifiedTime, permissions);
    }

    /**
     * 添加普通目录，判断过滤监听事件
     */
    public static FileSystemItemModel addFile(Collection<FileItemModel> directories, Pair<File, Uri> uriPair, String permissions) {
        var childFile = uriPair.getFirst();
        boolean hidden = childFile.isHidden();
        String name = childFile.getName();
        long lastModifiedTime = childFile.lastModified();
        String extension = FileUtility.getExtension(name);
        long length = childFile.length();
        return addFile(directories, uriPair.getSecond(), name, hidden, lastModifiedTime, extension, permissions, length);
    }
}
