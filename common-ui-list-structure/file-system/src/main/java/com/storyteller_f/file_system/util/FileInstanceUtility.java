package com.storyteller_f.file_system.util;

import androidx.documentfile.provider.DocumentFile;

import com.storyteller_f.file_system.model.DirectoryItemModel;
import com.storyteller_f.file_system.model.FileItemModel;
import com.storyteller_f.file_system.model.FileSystemItemModel;
import com.storyteller_f.file_system.model.TorrentFileItemModel;

import java.io.File;
import java.util.Collection;
import java.util.List;

public class FileInstanceUtility {

    /**
     * 添加普通文件，判断过滤监听事件
     *
     * @param files            填充目的地
     * @param hidden           是否是隐藏文件
     * @param name             文件名
     * @param absolutePath     绝对路径
     * @param lastModifiedTime 上次访问时间
     * @param extension        文件扩展
     * @return 返回添加的文件
     */
    public static FileItemModel addFile(Collection<FileItemModel> files, boolean hidden, String name, String absolutePath, long lastModifiedTime, String extension, String permission, long size) {
        FileItemModel fileItemModel;
        if ("torrent".equals(extension)) {
            fileItemModel = new TorrentFileItemModel(name, absolutePath, hidden, lastModifiedTime, false);
        } else {
            fileItemModel = new FileItemModel(name, absolutePath, hidden, lastModifiedTime, extension, false);
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
     * @param isHiddenFile     是否是隐藏文件
     * @param directoryName    文件夹名
     * @param absolutePath     绝对路径
     * @param lastModifiedTime 上次访问时间
     * @return 如果客户端不允许添加，返回null
     */
    public static FileSystemItemModel addDirectory(Collection<DirectoryItemModel> directories, boolean isHiddenFile, String directoryName, String absolutePath, long lastModifiedTime, String permissions) {
        DirectoryItemModel e = new DirectoryItemModel(directoryName, absolutePath, isHiddenFile, lastModifiedTime, false);
        e.setPermissions(permissions);
        if (directories.add(e)) return e;
        return null;
    }

    /**
     * 添加普通目录，判断过滤监听事件
     *
     * @param directories    填充目的地
     * @param childDirectory 当前目录下的子文件夹
     */
    public static FileSystemItemModel addDirectory(Collection<DirectoryItemModel> directories, File childDirectory, String permissions) {
        boolean hidden = childDirectory.isHidden();
        String absolutePath = childDirectory.getAbsolutePath();
        String name = childDirectory.getName();
        long lastModifiedTime = childDirectory.lastModified();
        return addDirectory(directories, hidden, name, absolutePath, lastModifiedTime, permissions);
    }

    /**
     * 添加普通目录，判断过滤监听事件
     *
     * @param directories 填充目的地
     * @param childFile   当前目录下的子文件夹
     */
    public static FileSystemItemModel addFile(Collection<FileItemModel> directories, File childFile, String permissions) {
        boolean hidden = childFile.isHidden();
        String name = childFile.getName();
        String absolutePath = childFile.getAbsolutePath();
        long lastModifiedTime = childFile.lastModified();
        String extension = FileUtility.getExtension(name);
        long length = childFile.length();
        return addFile(directories, hidden, name, absolutePath, lastModifiedTime, extension, permissions, length);
    }


    public static void adDirectory(List<DirectoryItemModel> directories, String absPath, String permissions, DocumentFile documentFile) {
        String name = documentFile.getName();
        boolean isHiddenFile = name.startsWith(".");
        FileInstanceUtility.addDirectory(directories, isHiddenFile, name, absPath, documentFile.lastModified(), permissions);
    }

    public static FileItemModel addFile(List<FileItemModel> files, String absPath, String permissions, DocumentFile documentFile) {
        String name = documentFile.getName();
        boolean isHiddenFile = name.startsWith(".");
        String extension = FileUtility.getExtension(name);
        return FileInstanceUtility.addFile(files, isHiddenFile, name, absPath, documentFile.lastModified(), extension, permissions, documentFile.length());
    }
}
