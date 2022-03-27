package com.storyteller_f.file_system.instance;


import com.storyteller_f.file_system.Filter;
import com.storyteller_f.file_system.model.DirectoryItemModel;
import com.storyteller_f.file_system.model.FileItemModel;
import com.storyteller_f.file_system.model.FileSystemItemModelLite;
import com.storyteller_f.file_system.model.FilesAndDirectories;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;

public abstract class FileInstance {
    public final static int file_operate_type_move_delete = 1;
    public final static int file_operate_type_copy = 2;
    public final static int file_operate_type_delete = 3;
    //在线程下执行才有意义
    protected String path;
    protected String name;
    protected Filter filter;

    /**
     * @param filter 遍历文件夹用的
     * @param path   路径
     */
    public FileInstance(Filter filter, String path) {
        this.filter = filter;
        this.path = path;
        initName(path);
    }

    public FileInstance(String path) {
        this.path = path;
        initName(path);
    }

    public FileInstance() {

    }

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


    public abstract FileItemModel getFile();

    public abstract DirectoryItemModel getDirectory();

    private void initName(String path) {
        name = new File(path).getName();
    }

    public String getName() {
        return name;
    }

    /**
     * 执行此方法前应该自行判断文件是否存在，以及文件类型，否则产生无法预测的结果
     *
     * @return 文件字节长度
     */
    public abstract long getFileLength();

    public abstract BufferedOutputStream getBufferedOutputStream() throws Exception;

    public abstract BufferedInputStream getBufferedInputSteam() throws Exception;

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

    /**
     * 应该仅用于目录
     *
     * @return 返回所有的文件和目录
     */
    public abstract FilesAndDirectories list();

    /**
     * 是否是文件
     *
     * @return true 代表是文件
     * @throws Exception
     */
    public abstract boolean isFile() throws Exception;

    protected boolean needStop() {
        return Thread.currentThread().isInterrupted();
    }

    /**
     * 是否存在
     *
     * @return true代表存在
     */
    public abstract boolean exists();

    //TODO 如果目录末尾是一个/，那他只能是目录
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
     *
     * @return 返回他的文件夹
     * @throws Exception 无法获得父文件夹
     */
    public abstract FileInstance toParent() throws Exception;

    /**
     * 移动指针，把当前对象指向他的父文件夹
     *
     * @throws Exception 无法变成父文件夹
     */
    public abstract void changeToParent() throws Exception;

    public abstract long getDirectorySize();

    public String getPath() {
        return path;
    }

    public abstract boolean createFile() throws IOException;

    public abstract boolean isHide();

    public abstract boolean createDirectory() throws IOException;


    /**
     * 调用者只能是一个路径
     * 如果目标文件或者文件夹不存在，将会自动创建，因为在这种状态下，新建文件速度快，特别是外部存储目录
     *
     * @param name     名称
     * @param reCreate 是否创建
     * @return 返回子对象
     */
    public abstract FileInstance toChild(String name, boolean isFile, boolean reCreate) throws Exception;

    public abstract void changeToChild(String name, boolean isFile, boolean reCreate) throws Exception;

    /**
     * 基本上完成的工作是构造函数应该做的
     * 如果文件不存在也不会创建，因为在这种状态下，创建文件没有优势
     * 需要自行判断是否需要判断
     *
     * @param path 新的文件路径，路径的根应该和当前对象符合，如果需要跨根跳转，需要使用FileInstanceFactory完成
     */
    public abstract void changeTo(String path);

    /**
     * 获取父路径
     *
     * @return 父路径
     */
    public abstract String getParent();

    public abstract FilesAndDirectories listSafe();

    public void destroy() {
        name = null;
        path = null;
        filter = null;
    }
}
