package com.storyteller_f.file_system.instance.local;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;


import com.storyteller_f.file_system.Filter;
import com.storyteller_f.file_system.instance.FileInstance;
import com.storyteller_f.file_system.model.DirectoryItemModel;
import com.storyteller_f.file_system.model.FileItemModel;
import com.storyteller_f.file_system.model.FileSystemItemModel;
import com.storyteller_f.file_system.model.FilesAndDirectories;
import com.storyteller_f.file_system.model.TorrentFileModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Locale;

/**
 * 定义接口，方法
 */
@SuppressWarnings("ALL")
public abstract class LocalFileInstance extends FileInstance {

    private static final String TAG = "FileInstance";
    protected Context context;

    /**
     * @param filter 遍历文件夹用的
     * @param path   路径
     */
    public LocalFileInstance(Context context, Filter filter, String path) {
        super(filter, path);
        this.context = context;
    }

    public LocalFileInstance(Context context, String path) {
        super(path);
        this.context = context;
    }

    public LocalFileInstance(Context context) {
        super();
        this.context = context;
    }

    /**
     * 推荐使用这种方式获取
     *
     * @return 返回列表
     */
    public FilesAndDirectories listWalk() throws IOException {
        Log.d(TAG, "listWalk() called");
        ArrayList<FileItemModel> files = new ArrayList<>();
        ArrayList<DirectoryItemModel> directories = new ArrayList<>();
        return getFilesAndDirectoriesOnWalk(files, directories);
    }

    public FilesAndDirectories listSafe() {
        if (!path.endsWith("/")) {
            Log.w(TAG, "listSafe: path:" + path);
        }
        try {
            return listWalk();
        } catch (Exception e) {
            e.printStackTrace();
            return list();
        }
    }


    /**
     * 添加detail 类型的文件夹
     *
     * @param directories 填充位置
     * @param parent      父文件夹
     * @param sub         当前文件夹下的文件
     * @param detail      详情
     * @param hidden      是否是隐藏文件
     */
    protected void addDetailDirectoryByCmd(ArrayList<DirectoryItemModel> directories, String parent, File sub, String detail, boolean hidden) {
        if (filter == null || filter != null && filter.onPath(parent, sub.getAbsolutePath(), false)) {
            DirectoryItemModel fileItemModel = new DirectoryItemModel(sub.getName(), sub.getAbsolutePath(), hidden, sub.lastModified());
            fileItemModel.setDetail(detail);
            directories.add(fileItemModel);
        }
    }

    /**
     * 添加detail 类型的file
     *
     * @param files   填充目的地
     * @param parent  父文件夹
     * @param subFile 当前文件夹下的子文件
     * @param detail  详情
     * @param hidden  是否是隐藏文件
     */
    protected void addDetailFileByCmd(ArrayList<FileItemModel> files, String parent, File subFile, String detail, boolean hidden) {
        if (filter == null || filter != null && filter.onPath(parent, subFile.getAbsolutePath(), true)) {
            String extension = getExtension(subFile.getName());
            long time = subFile.lastModified();
            if ("torrent".equals(extension)) {
                files.add(new TorrentFileModel(subFile.getName(), subFile.getAbsolutePath(), hidden, time));
                return;
            }
            FileItemModel fileItemModel = new FileItemModel(subFile, hidden, time, extension);
            fileItemModel.setDetail(detail);
            files.add(fileItemModel);
        }
    }

    /**
     * 添加普通目录，判断过滤监听事件
     *
     * @param directories  填充目的地
     * @param parent       父文件夹
     * @param hidden       是否是隐藏文件
     * @param name         文件夹名
     * @param absolutePath 绝对路径
     * @param time         上次访问时间
     * @return 如果客户端不允许添加，返回null
     */
    protected FileSystemItemModel addDirectory(ArrayList<DirectoryItemModel> directories, String parent, boolean hidden, String name, String absolutePath, long time) {
        if (filter == null || filter != null && filter.onPath(parent, absolutePath, false)) {
            if (!absolutePath.endsWith("/")) {
                absolutePath += "/";
            }
            DirectoryItemModel e = new DirectoryItemModel(name, absolutePath, hidden, time);
            directories.add(e);
            return e;
        }
        return null;
    }


    /**
     * 添加普通文件，判断过滤监听事件
     *
     * @param files        填充目的地
     * @param parent       父文件夹
     * @param hidden       是否是隐藏文件
     * @param name         文件名
     * @param absolutePath 绝对路径
     * @param time         上次访问时间
     * @param extension    文件扩展
     * @return 返回添加的文件
     */
    protected FileItemModel addFile(ArrayList<FileItemModel> files, String parent, boolean hidden, String name, String absolutePath, long time, String extension) {
        if (filter == null || filter != null && filter.onPath(parent, absolutePath, true)) {
            if ("torrent".equals(extension)) {
                TorrentFileModel torrentFileModel = new TorrentFileModel(name, absolutePath, hidden, time);
                files.add(torrentFileModel);
                return torrentFileModel;
            }
            FileItemModel e = new FileItemModel(name, absolutePath, hidden, time, extension);
            files.add(e);
            return e;
        }
        return null;
    }

    /**
     * 添加普通目录，判断过滤监听事件
     *
     * @param directories 填充目的地
     * @param parent      父文件夹
     * @param directory   当前目录下的子文件夹
     * @param hidden      是否是隐藏文件
     */
    protected FileSystemItemModel addDirectoryByFileObject(ArrayList<DirectoryItemModel> directories, String parent, File directory) {
        return addDirectory(directories, parent, directory.isHidden(), directory.getName(), directory.getAbsolutePath(), directory.lastModified());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    protected FilesAndDirectories getFilesAndDirectoriesOnWalk(final ArrayList<FileItemModel> files, final ArrayList<DirectoryItemModel> directories) throws IOException {
        Files.walkFileTree(new File(path).toPath(), EnumSet.of(FileVisitOption.FOLLOW_LINKS), 1, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (needStop()) return FileVisitResult.TERMINATE;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (needStop()) return FileVisitResult.TERMINATE;
                File subFile = file.toFile();
                FileSystemItemModel fileSystemItemModel;
                if (subFile.isFile()) {
                    FileItemModel fileSystemItemModel1 = addFile(files, path, subFile.isHidden(), subFile.getName(), subFile.getAbsolutePath(), subFile.lastModified(), getExtension(subFile.getName()));
                    if (fileSystemItemModel1 != null)
                        fileSystemItemModel1.setSize(subFile.length());
                    fileSystemItemModel = fileSystemItemModel1;
                } else {
                    fileSystemItemModel = addDirectoryByFileObject(directories, path, subFile);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    BasicFileAttributes basicFileAttributes = null;
                    try {
                        basicFileAttributes = Files.readAttributes(file.toAbsolutePath(), BasicFileAttributes.class);
                        if (fileSystemItemModel != null) {
                            fileSystemItemModel.setCreatedTime(basicFileAttributes.creationTime().toMillis());
                            fileSystemItemModel.setLastAccessTime(basicFileAttributes.lastAccessTime().toMillis());
                        }
                    } catch (IOException e) {
                        if (e instanceof AccessDeniedException) {
                            Log.e(TAG, "visitFile: " + e.getMessage());
                        } else
                            e.printStackTrace();
                    }
                }
                if (fileSystemItemModel != null) {
                    boolean w = subFile.canWrite();
                    boolean r = subFile.canRead();
                    boolean x = subFile.canExecute();
                    String detail = String.format(Locale.CHINA, "%c%c%c%c", (subFile.isFile() ? '-' : 'd'), (r ? 'r' : '-'), (w ? 'w' : '-'), (x ? 'e' : '-'));
                    fileSystemItemModel.setDetail(detail);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                if (needStop()) return FileVisitResult.TERMINATE;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                if (needStop()) return FileVisitResult.TERMINATE;
                return FileVisitResult.CONTINUE;
            }
        });
        return new FilesAndDirectories(files, directories);
    }

}
