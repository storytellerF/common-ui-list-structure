package com.storyteller_f.file_system.instance;


import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.core.util.ObjectsCompat;

import com.storyteller_f.file_system.model.DirectoryItemModel;
import com.storyteller_f.file_system.model.FileItemModel;
import com.storyteller_f.file_system.model.FileSystemItemModel;
import com.storyteller_f.file_system.model.FilesAndDirectories;
import com.storyteller_f.multi_core.StoppableTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import kotlin.Pair;

/**
 * notice 如果需要给name 设置值，那就需要提供path。或者自行处理
 */
public abstract class FileInstance {
    //todo 考虑使用suspend 替代
    protected StoppableTask task;
    @NonNull
    public Uri uri;

    public FileInstance(@NonNull Uri uri) {
        this.uri = uri;
        assert getPath().trim().length() != 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileInstance that = (FileInstance) o;
        return ObjectsCompat.equals(uri, that.uri);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(uri);
    }

    @WorkerThread
    public abstract FileItemModel getFile();

    @WorkerThread
    public abstract DirectoryItemModel getDirectory();

    @WorkerThread
    public FileSystemItemModel getFileSystemItem() throws Exception {
        if (isFile()) return getFile();
        else return getDirectory();
    }

    protected Pair<File, Uri> child(String it) {
        var file = new File(getPath(), it);
        var child = uri.buildUpon().path(file.getAbsolutePath()).build();
        return new Pair<>(file, child);
    }

    public String getName() {
        return new File(getPath()).getName();
    }

    /**
     * 执行此方法前应该自行判断文件是否存在，以及文件类型，否则产生无法预测的结果
     *
     * @return 文件字节长度
     */
    @WorkerThread
    abstract public long getFileLength();

    @WorkerThread
    public abstract FileInputStream getFileInputStream() throws FileNotFoundException;
    @WorkerThread
    public abstract FileOutputStream getFileOutputStream() throws FileNotFoundException;

    //todo getChannel
    //todo file descriptor

    /**
     * 应该仅用于目录。可能会抛出异常，内部不会处理。
     */
    @WorkerThread
    protected abstract void listInternal(@NonNull List<FileItemModel> fileItems, @NonNull List<DirectoryItemModel> directoryItems) throws Exception;

    @WorkerThread
    public FilesAndDirectories list() throws Exception {
        FilesAndDirectories filesAndDirectories = new FilesAndDirectories(buildFilesContainer(), buildDirectoryContainer());
        listInternal(filesAndDirectories.getFiles(), filesAndDirectories.getDirectories());
        return filesAndDirectories;
    }

    private List<FileItemModel> buildFilesContainer() {
        return new ArrayList<>() {

        };
    }

    private List<DirectoryItemModel> buildDirectoryContainer() {
        return new ArrayList<>() {

        };
    }

    /**
     * 是否是文件
     *
     * @return true 代表是文件
     */
    @WorkerThread
    public abstract boolean isFile() throws Exception;

    protected boolean needStop() {
        if (Thread.currentThread().isInterrupted()) return true;
        if (task != null) return task.needStop();
        else return false;
    }

    /**
     * 是否存在
     *
     * @return true代表存在
     */
    @WorkerThread
    public abstract boolean exists();
    @WorkerThread
    public abstract boolean isDirectory() throws Exception;

    /**
     * 删除当前文件
     *
     * @return 返回是否删除成功
     */
    @WorkerThread
    public abstract boolean deleteFileOrEmptyDirectory() throws Exception;

    /**
     * 重命名当前文件
     *
     * @param newName 新的文件名，不包含路径
     * @return 返回是否重命名成功
     */
    @WorkerThread
    public abstract boolean rename(String newName);

    /**
     * 移动指针，指向他的父文件夹
     * 不应该考虑能否转换成功
     *
     * @return 返回他的文件夹
     * @throws Exception 无法获得父文件夹
     */
    @WorkerThread
    public abstract FileInstance toParent() throws Exception;

    /**
     * 移动指针，把当前对象指向他的父文件夹
     * 不应该考虑能否转换成功
     *
     * @throws Exception 无法变成父文件夹
     */
    @WorkerThread
    public abstract void changeToParent() throws Exception;

    @WorkerThread
    public abstract long getDirectorySize();

    @WorkerThread
    public String getPath() {
        return uri.getPath();
    }
    @WorkerThread
    public abstract boolean createFile() throws IOException;
    @WorkerThread
    public abstract boolean isHidden();
    @WorkerThread
    public abstract boolean createDirectory() throws IOException;


    /**
     * 调用者只能是一个路径
     * 如果目标文件或者文件夹不存在，将会自动创建，因为在这种状态下，新建文件速度快，特别是外部存储目录
     * 不应该考虑能否转换成功
     *
     * @param name                名称
     * @return 返回子对象
     */
    @WorkerThread
    public abstract FileInstance toChild(@NonNull String name, FileCreatePolicy policy) throws Exception;

    /**
     * 不应该考虑能否转换成功
     */
    @WorkerThread
    public abstract void changeToChild(@NonNull String name, FileCreatePolicy policy) throws Exception;

    /**
     * 基本上完成的工作是构造函数应该做的
     * 如果文件不存在也不会创建，因为在这种状态下，创建文件没有优势
     * 不应该考虑能否转换成功
     *
     * @param path 新的文件路径，路径的根应该和当前对象符合，如果需要跨根跳转，需要使用FileInstanceFactory完成
     */
    @WorkerThread
    public abstract void changeTo(@NonNull String path);

    /**
     * 获取父路径
     *
     * @return 父路径
     */
    @WorkerThread
    public String getParent() {
        return new File(getPath()).getParent();
    }


    @WorkerThread
    public boolean isSymbolicLink() {
        return false;
    }

    @WorkerThread
    public boolean isSoftLink() {
        return false;
    }

    @WorkerThread
    public boolean isHardLink() {
        return false;
    }
}
