package com.storyteller_f.file_system.instance.local;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.storyteller_f.file_system.instance.Create;
import com.storyteller_f.file_system.instance.FileCreatePolicy;
import com.storyteller_f.file_system.model.DirectoryItemModel;
import com.storyteller_f.file_system.model.FileItemModel;
import com.storyteller_f.file_system.model.FileSystemItemModel;
import com.storyteller_f.file_system.util.FileInstanceUtility;
import com.storyteller_f.file_system.util.FileUtility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import kotlin.Pair;

@SuppressWarnings({"unused", "RedundantSuppression"})
public class RegularLocalFileInstance extends LocalFileInstance {
    private static final String TAG = "ExternalFileInstance";
    private File file = new File(getPath());

    public RegularLocalFileInstance(Context context, Uri uri) {
        super(context, uri);
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
    public LocalFileInstance toChild(@NonNull String name, FileCreatePolicy policy) throws Exception {
        File subFile = new File(file, name);
        Uri uri = getUri(subFile);
        RegularLocalFileInstance internalFileInstance = new RegularLocalFileInstance(context, uri);
        //检查目标文件是否存在
        checkChildExistsOtherwiseCreate(subFile, policy);
        return internalFileInstance;
    }

    private static Uri getUri(File subFile) {
        return new Uri.Builder().scheme("file").path(subFile.getPath()).build();
    }

    private void checkChildExistsOtherwiseCreate(File file, FileCreatePolicy policy) throws Exception {
        if (!exists()) {
            throw new Exception("当前文件或者文件夹不存在。path:" + getPath());
        } else if (isFile()) throw new Exception("当前是一个文件，无法向下操作");
        else if (!file.exists()) {
            if (policy instanceof Create) {
                if (((Create) policy).isFile()) {
                    if (!file.createNewFile()) throw new Exception("新建文件失败");
                } else if (!file.mkdirs()) throw new Exception("新建文件失败");
            } else throw new Exception("不存在，且不能创建");
        }
    }

    @Override
    public void changeToChild(@NonNull String name, FileCreatePolicy policy) throws Exception {
        Log.d(TAG, "changeToChild() called with: name = [" + name + "], policy = [" + policy + "]");
        checkChildExistsOtherwiseCreate(file, policy);
        file = new File(file, name);
    }

    @Override
    public void changeTo(@NonNull String path) {
        if (this.getPath().equals(path)) {
            return;
        }
        this.file = new File(path);
    }

    @Override
    public FileInputStream getFileInputStream() throws FileNotFoundException {
        return new FileInputStream(file);
    }

    @Override
    public FileOutputStream getFileOutputStream() throws FileNotFoundException {
        return new FileOutputStream(file);
    }

    public FileItemModel getFile() {
        FileItemModel fileItemModel = new FileItemModel(file.getName(), uri, file.isHidden(), file.lastModified(), false, FileUtility.getExtension(getName()));
        fileItemModel.editAccessTime(file);
        return fileItemModel;
    }

    public DirectoryItemModel getDirectory() {
        DirectoryItemModel directoryItemModel = new DirectoryItemModel(file.getName(), uri, file.isHidden(), file.lastModified(), false);
        directoryItemModel.editAccessTime(file);
        return directoryItemModel;
    }

    @Override
    public long getFileLength() {
        return file.length();
    }

    @Override
    @WorkerThread
    public void listInternal(@NonNull List<FileItemModel> fileItems, @NonNull List<DirectoryItemModel> directoryItems) {
        File[] listFiles = file.listFiles();//获取子文件

        if (listFiles != null) {
            for (File childFile : listFiles) {
                Pair<File, Uri> child = child(childFile.getName());
                String permissions = FileUtility.getPermissionStringByFile(childFile);
                FileSystemItemModel fileSystemItemModel;
                // 判断是否为文件夹
                if (childFile.isDirectory()) {
                    fileSystemItemModel = FileInstanceUtility.addDirectory(directoryItems, child, permissions);
                } else {
                    fileSystemItemModel = FileInstanceUtility.addFile(fileItems, child, permissions);
                }
                fileSystemItemModel.editAccessTime(childFile);
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
        return new RegularLocalFileInstance(context, getUri(file.getParentFile()));
    }

    @Override
    public void changeToParent() {
        file = file.getParentFile();
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

    @Override
    public boolean isSymbolicLink() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Files.isSymbolicLink(file.toPath());
        }
        try {
            return file.getAbsolutePath().equals(file.getCanonicalPath());
        } catch (IOException e) {
            return false;
        }
    }
}
