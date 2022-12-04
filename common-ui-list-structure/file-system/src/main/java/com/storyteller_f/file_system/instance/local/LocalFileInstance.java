package com.storyteller_f.file_system.instance.local;

import android.content.Context;
import android.os.Build;

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
                boolean w = childFile.canWrite();
                boolean r = childFile.canRead();
                boolean x = childFile.canExecute();
                String detail = String.format(Locale.CHINA, "%c%c%c%c", (childFile.isFile() ? '-' : 'd'), (r ? 'r' : '-'), (w ? 'w' : '-'), (x ? 'e' : '-'));
                if (childFile.isFile()) {
                    fileSystemItemModel = addFile(files, path, childFile.isHidden(), childFile.getName(), childFile.getAbsolutePath(), childFile.lastModified(), getExtension(childFile.getName()), detail);
                    if (fileSystemItemModel != null)
                        fileSystemItemModel.setSize(childFile.length());
                } else {
                    fileSystemItemModel = addDirectory(directories, path, childFile, detail);
                }
                editAccessTime(childFile, fileSystemItemModel);
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
