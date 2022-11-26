package com.storyteller_f.file_system.instance.local;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;

import com.storyteller_f.file_system.Filter;
import com.storyteller_f.file_system.instance.BaseContextFileInstance;
import com.storyteller_f.file_system.model.DirectoryItemModel;
import com.storyteller_f.file_system.model.FileItemModel;
import com.storyteller_f.file_system.model.FileSystemItemModel;
import com.storyteller_f.file_system.model.FilesAndDirectories;

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
public abstract class LocalFileInstance extends BaseContextFileInstance {

    private static final String TAG = "FileInstance";

    /**
     * @param filter 遍历文件夹用的
     * @param path   路径
     */
    public LocalFileInstance(Context context, Filter filter, String path) {
        super(context, filter, path);
    }

    public LocalFileInstance(Context context, String path) {
        super(context, path);
    }

    public LocalFileInstance(Context context) {
        super(context);
    }

    @NonNull
    @WorkerThread
    public FilesAndDirectories listSafe() {
        try {
            ArrayList<FileItemModel> files = new ArrayList<>();
            ArrayList<DirectoryItemModel> directories = new ArrayList<>();
            return getFilesAndDirectoriesOnWalk(files, directories);
        } catch (Exception e) {
            e.printStackTrace();
            return list();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @WorkerThread
    private FilesAndDirectories getFilesAndDirectoriesOnWalk(final ArrayList<FileItemModel> files, final ArrayList<DirectoryItemModel> directories) throws IOException {
        Files.walkFileTree(new File(path).toPath(), EnumSet.of(FileVisitOption.FOLLOW_LINKS), 1, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (needStop()) return FileVisitResult.TERMINATE;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (needStop()) return FileVisitResult.TERMINATE;
                File childFile = file.toFile();
                FileSystemItemModel fileSystemItemModel;
                if (childFile.isFile()) {
                    FileItemModel fileSystemItemModel1 = addFile(files, path, childFile.isHidden(), childFile.getName(), childFile.getAbsolutePath(), childFile.lastModified(), getExtension(childFile.getName()));
                    if (fileSystemItemModel1 != null)
                        fileSystemItemModel1.setSize(childFile.length());
                    fileSystemItemModel = fileSystemItemModel1;
                } else {
                    fileSystemItemModel = addDirectoryByFileObject(directories, path, childFile);
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
                    boolean w = childFile.canWrite();
                    boolean r = childFile.canRead();
                    boolean x = childFile.canExecute();
                    String detail = String.format(Locale.CHINA, "%c%c%c%c", (childFile.isFile() ? '-' : 'd'), (r ? 'r' : '-'), (w ? 'w' : '-'), (x ? 'e' : '-'));
                    fileSystemItemModel.setPermissions(detail);
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
