package com.storyteller_f.file_system.instance;


import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.util.ObjectsCompat;

import com.storyteller_f.file_system.Filter;
import com.storyteller_f.file_system.model.DirectoryItemModel;
import com.storyteller_f.file_system.model.FileItemModel;
import com.storyteller_f.file_system.model.FileSystemItemModel;
import com.storyteller_f.file_system.model.FilesAndDirectories;
import com.storyteller_f.file_system.model.TorrentFileItemModel;
import com.storyteller_f.multi_core.StoppableTask;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * notice 如果需要给name 设置值，那就需要提供path。或者自行处理
 */
public abstract class FileInstance {
    private static final String TAG = "FileInstance";
    //在线程下执行才有意义
    protected String path;
    protected String name;
    protected Filter filter;
    /**
     * 仅用来标识对象，有些情况下是不能够操作的
     */
    protected File file;
    protected StoppableTask task;

    /**
     * @param filter 遍历文件夹用的
     * @param path   路径
     */
    public FileInstance(Filter filter, String path) {
        this.filter = filter;
        this.path = path;
        initName();
    }

    public FileInstance(String path) {
        this.path = path;
        initName();
    }

    @Nullable
    public static String getExtension(String name) {
        String extension;
        int index = name.lastIndexOf('.');
        if (index != -1) {
            extension = name.substring(index + 1);
        } else {
            extension = null;
        }
        return extension;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileInstance that = (FileInstance) o;
        return ObjectsCompat.equals(path, that.path) && ObjectsCompat.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(path, name);
    }

    protected void editAccessTime(File childFile, FileSystemItemModel fileSystemItemModel) {
        if (fileSystemItemModel != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    BasicFileAttributes basicFileAttributes = Files.readAttributes(childFile.toPath(), BasicFileAttributes.class);
                    fileSystemItemModel.setCreatedTime(basicFileAttributes.creationTime().toMillis());
                    fileSystemItemModel.setLastAccessTime(basicFileAttributes.lastAccessTime().toMillis());
                } catch (IOException e) {
                    Log.w(TAG, "list: 获取BasicFileAttribute失败" + childFile.getAbsolutePath());
                }
            }
        }
    }

    public FileItemModel getFile() {
        FileItemModel fileItemModel = new FileItemModel(name, path, file.isHidden(), file.lastModified(), getExtension(name));
        editAccessTime(file, fileItemModel);
        return fileItemModel;
    }

    public DirectoryItemModel getDirectory() {
        DirectoryItemModel directoryItemModel = new DirectoryItemModel(name, path, file.isHidden(), file.lastModified());
        editAccessTime(file, directoryItemModel);
        return directoryItemModel;
    }

    public FileSystemItemModel getFileSystemItem() throws Exception {
        if (isFile()) return getFile();
        else return getDirectory();
    }

    protected void initName() {
        file = new File(this.path);
        name = file.getName();
    }

    public String getName() {
        return name;
    }

    /**
     * 执行此方法前应该自行判断文件是否存在，以及文件类型，否则产生无法预测的结果
     *
     * @return 文件字节长度
     */
    public long getFileLength() {
        return file.length();
    }

    public BufferedOutputStream getBufferedOutputStream() throws Exception {
        return new BufferedOutputStream(getFileOutputStream());
    }

    public BufferedInputStream getBufferedInputSteam() throws Exception {
        return new BufferedInputStream(new FileInputStream(path));
    }

    /**
     * 应该仅用于文件，可以使用readLine 方法
     */
    public abstract BufferedReader getBufferedReader() throws Exception;

    public abstract BufferedWriter getBufferedWriter() throws Exception;

    public InputStreamReader getInputStreamReader(String charset) throws Exception {
        return new InputStreamReader(getBufferedInputSteam(), charset);
    }

    public OutputStreamWriter getOutputStreamWriter(String charset) throws Exception {
        return new OutputStreamWriter(getBufferedOutputStream(), charset);
    }

    public InputStreamReader getInputStreamReader(Charset charset) throws Exception {
        return new InputStreamReader(getBufferedInputSteam(), charset);
    }

    public OutputStreamWriter getOutputStreamWriter(Charset charset) throws Exception {
        return new OutputStreamWriter(getBufferedOutputStream(), charset);
    }

    public abstract FileInputStream getFileInputStream() throws FileNotFoundException;

    public abstract FileOutputStream getFileOutputStream() throws FileNotFoundException;

    /**
     * 应该仅用于目录。可能会抛出异常，内部不会处理。
     *
     */
    @WorkerThread
    protected abstract void list(List<FileItemModel> fileItems, List<DirectoryItemModel> directoryItems);

    public FilesAndDirectories listSafe() {
        FilesAndDirectories filesAndDirectories = new FilesAndDirectories(buildFilesContainer(), buildDirectoryContainer());
        list(filesAndDirectories.getFiles(), filesAndDirectories.getDirectories());
        return filesAndDirectories;
    }

    private List<FileItemModel> buildFilesContainer() {
        return new ArrayList<>() {
            @Override
            public boolean add(FileItemModel fileItemModel) {
                if (checkWhenAdd(path, fileItemModel.getFullPath(), true)) return super.add(fileItemModel);
                return false;
            }

            @Override
            public void add(int index, FileItemModel element) {
                if (checkWhenAdd(path, element.getFullPath(), true)) super.add(index, element);
            }

            @Override
            public boolean addAll(@NonNull Collection<? extends FileItemModel> c) {
                return false;
            }

            @Override
            public boolean addAll(int index, @NonNull Collection<? extends FileItemModel> c) {
                return false;
            }
        };
    }

    private List<DirectoryItemModel> buildDirectoryContainer() {
        return new ArrayList<>() {
            @Override
            public void add(int index, DirectoryItemModel element) {
                if (checkWhenAdd(path, element.getFullPath(), false))
                    super.add(index, element);
            }

            @Override
            public boolean add(DirectoryItemModel directoryItemModel) {
                if (checkWhenAdd(path, directoryItemModel.getFullPath(), false))
                    return super.add(directoryItemModel);
                return false;
            }

            @Override
            public boolean addAll(int index, @NonNull Collection<? extends DirectoryItemModel> c) {
                return false;
            }

            @Override
            public boolean addAll(@NonNull Collection<? extends DirectoryItemModel> c) {
                return false;
            }
        };
    }

    /**
     * 是否是文件
     *
     * @return true 代表是文件
     */
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
    public abstract boolean exists();

    public abstract boolean isDirectory() throws Exception;

    /**
     * 删除当前文件
     *
     * @return 返回是否删除成功
     */
    public abstract boolean deleteFileOrEmptyDirectory() throws Exception;

    /**
     * 重命名当前文件
     *
     * @param newName 新的文件名，不包含路径
     * @return 返回是否重命名成功
     */
    public abstract boolean rename(String newName);

    /**
     * 移动指针，指向他的父文件夹
     * 不应该考虑能否转换成功
     *
     * @return 返回他的文件夹
     * @throws Exception 无法获得父文件夹
     */
    public abstract FileInstance toParent() throws Exception;

    /**
     * 移动指针，把当前对象指向他的父文件夹
     * 不应该考虑能否转换成功
     *
     * @throws Exception 无法变成父文件夹
     */
    public abstract void changeToParent() throws Exception;

    @WorkerThread
    public abstract long getDirectorySize();

    public String getPath() {
        return path;
    }

    public abstract boolean createFile() throws IOException;

    public abstract boolean isHidden();

    public abstract boolean createDirectory() throws IOException;


    /**
     * 调用者只能是一个路径
     * 如果目标文件或者文件夹不存在，将会自动创建，因为在这种状态下，新建文件速度快，特别是外部存储目录
     * 不应该考虑能否转换成功
     *
     * @param name                名称
     * @param createWhenNotExists 是否创建
     * @return 返回子对象
     */
    public abstract FileInstance toChild(@NonNull String name, boolean isFile, boolean createWhenNotExists) throws Exception;

    /**
     * 不应该考虑能否转换成功
     */
    public abstract void changeToChild(@NonNull String name, boolean isFile, boolean createWhenNotExists) throws Exception;

    /**
     * 基本上完成的工作是构造函数应该做的
     * 如果文件不存在也不会创建，因为在这种状态下，创建文件没有优势
     * 不应该考虑能否转换成功
     *
     * @param path 新的文件路径，路径的根应该和当前对象符合，如果需要跨根跳转，需要使用FileInstanceFactory完成
     */
    public abstract void changeTo(@NonNull String path);

    /**
     * 获取父路径
     *
     * @return 父路径
     */
    public String getParent() {
        return file.getParent();
    }

    /**
     * 添加普通目录，判断过滤监听事件
     *
     * @param directories    填充目的地
     * @param parent         父文件夹
     * @param childDirectory 当前目录下的子文件夹
     */
    protected FileSystemItemModel addDirectory(Collection<DirectoryItemModel> directories, String parent, File childDirectory, String permissions) {
        boolean hidden = childDirectory.isHidden();
        String absolutePath = childDirectory.getAbsolutePath();
        String name = childDirectory.getName();
        long lastModifiedTime = childDirectory.lastModified();
        return addDirectory(directories, parent, hidden, name, absolutePath, lastModifiedTime, permissions);
    }

    /**
     * 添加普通目录，判断过滤监听事件
     *
     * @param directories 填充目的地
     * @param parent      父文件夹
     * @param childFile   当前目录下的子文件夹
     */
    protected FileSystemItemModel addFile(Collection<FileItemModel> directories, String parent, File childFile, String permissions) {
        boolean hidden = childFile.isHidden();
        String name = childFile.getName();
        String absolutePath = childFile.getAbsolutePath();
        long lastModifiedTime = childFile.lastModified();
        String extension = getExtension(name);
        long length = childFile.length();
        return addFile(directories, parent, hidden, name, absolutePath, lastModifiedTime, extension, permissions, length);
    }

    private boolean checkWhenAdd(String parent, String absolutePath, boolean isFile) {
        return filter == null || filter.onPath(parent, absolutePath, isFile);
    }

    /**
     * 添加普通文件，判断过滤监听事件
     *
     * @param files            填充目的地
     * @param parent           父文件夹
     * @param hidden           是否是隐藏文件
     * @param name             文件名
     * @param absolutePath     绝对路径
     * @param lastModifiedTime 上次访问时间
     * @param extension        文件扩展
     * @return 返回添加的文件
     */
    protected FileItemModel addFile(Collection<FileItemModel> files, String parent, boolean hidden, String name, String absolutePath, long lastModifiedTime, String extension, String permission, long size) {
        FileItemModel fileItemModel;
        if ("torrent".equals(extension)) {
            fileItemModel = new TorrentFileItemModel(name, absolutePath, hidden, lastModifiedTime);
        } else {
            fileItemModel = new FileItemModel(name, absolutePath, hidden, lastModifiedTime, extension);
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
     * @param parentDirectory  父文件夹
     * @param isHiddenFile     是否是隐藏文件
     * @param directoryName    文件夹名
     * @param absolutePath     绝对路径
     * @param lastModifiedTime 上次访问时间
     * @return 如果客户端不允许添加，返回null
     */
    protected FileSystemItemModel addDirectory(Collection<DirectoryItemModel> directories, String parentDirectory, boolean isHiddenFile, String directoryName, String absolutePath, long lastModifiedTime, String permissions) {
        DirectoryItemModel e = new DirectoryItemModel(directoryName, absolutePath, isHiddenFile, lastModifiedTime);
        e.setPermissions(permissions);
        if (directories.add(e)) return e;
        return null;
    }

}
