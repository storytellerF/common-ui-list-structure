package com.storyteller_f.file_system.instance.local;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.storyteller_f.file_system.Filter;
import com.storyteller_f.file_system.model.DirectoryItemModel;
import com.storyteller_f.file_system.model.FileItemModel;
import com.storyteller_f.file_system.model.FileSystemItemModel;
import com.storyteller_f.file_system.model.FilesAndDirectories;
import com.storyteller_f.file_system.util.FileUtility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class RegularLocalFileInstance extends LocalFileInstance {
    private static final String TAG = "ExternalFileInstance";

    public RegularLocalFileInstance(Context context, Filter filter, String path) {
        super(context, filter, path);
    }

    public RegularLocalFileInstance(Context context, String path) {
        super(context, path);
    }

    @Override
    public boolean createFile() throws IOException {
        if (file.exists()) return true;
        return file.createNewFile();
    }

    @Override
    public boolean isHidden() {
        return file.isHidden();
    }

    @Override
    public boolean createDirectory() {
        if (file.exists()) return true;
        return file.mkdirs();
    }

    @Override
    public LocalFileInstance toChild(@NonNull String name, boolean isFile, boolean createWhenNotExists) throws Exception {
        File subFile = new File(file, name);
        RegularLocalFileInstance internalFileInstance = new RegularLocalFileInstance(context, filter, subFile.getAbsolutePath());
        //检查目标文件是否存在
        checkChildExistsOtherwiseCreate(subFile, isFile, createWhenNotExists);
        return internalFileInstance;
    }

    private void checkChildExistsOtherwiseCreate(File file, boolean isFile, boolean createWhenNotExists) throws Exception {
        if (exists()) if (isFile()) throw new Exception("当前是一个文件，无法向下操作");
        else if (file.exists()) {
            if (file.isFile() != isFile) throw new Exception("当前文件已经存在，并且类型不符合：" + file.isFile() + " " + isFile);
        } else if (createWhenNotExists) {
            if (isFile) {
                if (!file.createNewFile()) throw new Exception("新建文件失败");
            } else if (!file.mkdirs()) throw new Exception("新建文件失败");
        } else throw new Exception("不存在，且不能创建");
        else
            throw new Exception("当前文件或者文件夹不存在。path:" + path);
    }

    @Override
    public void changeToChild(@NonNull String name, boolean isFile, boolean createWhenNotExists) throws Exception {
        Log.d(TAG, "changeToChild() called with: name = [" + name + "], isFile = [" + isFile + "]");
        checkChildExistsOtherwiseCreate(file, isFile, createWhenNotExists);
        path = file.getAbsolutePath();
        initName();
    }

    @Override
    public void changeTo(@NonNull String path) {
        if (this.path.equals(path)) {
            return;
        }
        this.path = path;
        this.file = new File(path);
    }

    @Override
    public BufferedReader getBufferedReader() throws FileNotFoundException {
        return new BufferedReader(new FileReader(path));
    }

    @Override
    public BufferedWriter getBufferedWriter() throws IOException {
        return new BufferedWriter(new FileWriter(path));
    }

    @Override
    public FileInputStream getFileInputStream() throws FileNotFoundException {
        return new FileInputStream(file);
    }

    @Override
    public FileOutputStream getFileOutputStream() throws FileNotFoundException {
        return new FileOutputStream(file);
    }

    @Override
    @WorkerThread
    public void list(List<FileItemModel> fileItems, List<DirectoryItemModel> directoryItems) {
        File[] listFiles = file.listFiles();//获取子文件

        if (listFiles != null) {
            for (File childFile : listFiles) {
                String permissions = FileUtility.getPermissionStringByFile(childFile);
                FileSystemItemModel fileSystemItemModel;
                // 判断是否为文件夹
                if (childFile.isDirectory()) {
                    fileSystemItemModel = addDirectory(directoryItems, file.getAbsolutePath(), childFile, permissions);
                } else {
                    fileSystemItemModel = addFile(fileItems, file.getAbsolutePath(), childFile, permissions);
                }
                editAccessTime(childFile, fileSystemItemModel);
            }
        }
    }

    @Override
    public boolean isFile() {
        return file.isFile();
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public boolean isDirectory() {
        return file.isDirectory();
    }

    @Override
    public boolean deleteFileOrEmptyDirectory() {
        return file.delete();
    }

    @Override
    public boolean rename(String newName) {
        return file.renameTo(new File(newName));
    }

    @Override
    public LocalFileInstance toParent() {
        return new RegularLocalFileInstance(context, filter, file.getParent());
    }

    @Override
    public void changeToParent() {
        File parentFile = file.getParentFile();
        path = parentFile.getPath();
        name = parentFile.getName();
    }

    @Override
    public long getDirectorySize() {
        return getFileSize(file);
    }

    @WorkerThread
    private long getFileSize(File file) {
        long size = 0;
        File[] files = file.listFiles();
        if (files == null) return size;
        for (File f : files) {
            if (needStop()) break;
            if (f.isFile()) {
                size += f.length();
            } else {
                size += getFileSize(f);
            }
        }
        return size;
    }
}
