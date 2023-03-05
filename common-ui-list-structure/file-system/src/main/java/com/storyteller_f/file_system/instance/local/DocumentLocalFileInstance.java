package com.storyteller_f.file_system.instance.local;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.storyteller_f.file_system.FileInstanceFactory;
import com.storyteller_f.file_system.FileSystemUriSaver;
import com.storyteller_f.file_system.model.DirectoryItemModel;
import com.storyteller_f.file_system.model.FileItemModel;
import com.storyteller_f.file_system.util.FileInstanceUtility;
import com.storyteller_f.file_system.util.FileUtility;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DocumentLocalFileInstance extends LocalFileInstance {
    private static final String TAG = "DocumentLocalFileInstan";
    public static final String mountedKey = "mounted";
    public static final String emulatedKey = "emulated";
    protected DocumentFile current;
    /**
     * 用来标识对象所在区域，可能是外部，也可能是外部
     */
    String prefix;
    private String preferenceKey;

    public DocumentLocalFileInstance(Context context, String path, String preferenceKey, String fileSystemRoot) {
        super(context, path, fileSystemRoot);
        this.preferenceKey = preferenceKey;
        updatePrefix();
        initDocumentFile();
    }

    private void updatePrefix() {
        prefix = FileInstanceFactory.getPrefix(getPath(), context, fileSystemRoot);
    }

    /**
     * 初始化DocumentFile，初始化失败一般不影响获取目录,但是不可以对当前对象进行操作
     *
     * @return true 代表初始化成功
     */
    public boolean initDocumentFile() {
        current = getSpecialDocumentFile(getPath(), false, file.isFile());
        if (current == null) Log.e(TAG, "initDocumentFile: 初始化失败（未授权）" + getPath());
        return current != null;
    }

    /**
     * 获取指定目录的document file
     *
     * @param destination        目标地址
     * @param createWhenNoExists 决定在没有文件夹的时候是否创建文件夹
     * @return 返回目标文件
     */
    public DocumentFile getSpecialDocumentFile(String destination, boolean createWhenNoExists, boolean isFile) {
        if (!destination.startsWith(prefix)) {
            Log.e(TAG, "getCurrent: prefix 出错 " + prefix);
            return null;
        }
        Uri uri = FileSystemUriSaver.getInstance().savedUri(preferenceKey, context);
        if (uri == null) {
            Log.e(TAG, "getSpecialDocumentFile: uri is null");
            return null;
        }
        DocumentFile rootFile = DocumentFile.fromTreeUri(context, uri);
        if (rootFile == null) {
            Log.e(TAG, "getCurrent: rootFile is null");
            return null;
        }
        if (!rootFile.canRead()) {
            Log.e(TAG, "initDocumentFile: 初始化失败(权限过期) 不可读写 " + getPath());
            return null;
        }
        String truePath = destination.substring(prefix.length());//获取 SD卡名称后面的路径
        if (truePath.isEmpty() || (truePath.equals("/") && prefix.isEmpty())) return rootFile;
        String[] nameItemPath = truePath.substring(1).split("/");
        String[] paths;
        String[] files;
        if (isFile && createWhenNoExists) {
            paths = Arrays.copyOf(nameItemPath, nameItemPath.length - 1);
        } else paths = nameItemPath;
        if (isFile && createWhenNoExists) {
            files = new String[]{nameItemPath[nameItemPath.length - 1]};
        } else files = new String[0];
        DocumentFile temp = rootFile;
        for (String name : paths) {
            if (needStop()) break;
            DocumentFile foundFile = temp.findFile(name);
            if (foundFile == null) {
                if (!createWhenNoExists) return null;
                DocumentFile created = temp.createDirectory(name);
                if (created == null) return null;
                else temp = created;
            } else temp = foundFile;
        }
        //find file
        if (isFile && createWhenNoExists) {
            String fileName = files[0];
            DocumentFile file = temp.findFile(fileName);
            if (file == null) {
                return temp.createFile("*/*", fileName);
            }
        }
        return temp;
    }

    @Override
    public long getDirectorySize() {
        return getDocumentFileSize(current);
    }

    private long getDocumentFileSize(DocumentFile documentFile) {
        long size = 0;
        DocumentFile[] documentFiles = documentFile.listFiles();
        for (DocumentFile documentFi : documentFiles) {
            if (needStop()) break;
            if (documentFile.isFile()) {
                size += documentFi.length();
            } else {
                size += getDocumentFileSize(documentFi);
            }
        }
        return size;
    }

    @Override
    public String getParent() {
        DocumentFile parentFile = current.getParentFile();
        if (parentFile == null) {
            return null;
        }
        return parentFile.getUri().getPath();
    }

    public boolean createDirectory() {
        //todo 检查是不是真的directory
        if (current != null) return true;
        DocumentFile created = getSpecialDocumentFile(getPath(), true, false);
        if (created != null) {
            current = created;
            return true;
        }
        return false;
    }

    public boolean createFile() {
        if (current != null) return true;
        DocumentFile created = getSpecialDocumentFile(getPath(), true, true);
        if (created != null) {
            current = created;
            return true;
        }
        return false;
    }

    @Override
    public LocalFileInstance toChild(@NonNull String name, boolean isFile, boolean createWhenNotExists) throws Exception {
        if (!exists()) {
            Log.e(TAG, "toChild: 未经过初始化或者文件不存在：" + getPath());
            return null;
        }
        if (isFile()) {
            throw new Exception("当前是一个文件，无法向下操作");
        } else {
            DocumentLocalFileInstance instance = getInstance();
            instance.preferenceKey = preferenceKey;
            instance.prefix = prefix;
            instance.file = new File(file, name);
            instance.current = getNewCurrent(name, isFile, createWhenNotExists);
            return instance;
        }
    }

    /**
     * @param name                名称
     * @param isFile              是否是文件
     * @param createWhenNotExists 是否创建
     * @return 如果查找不到，而且不用创建，返回null
     * @throws Exception 会出现无法预计的结果时，不允许再次继续
     */
    public DocumentFile getNewCurrent(String name, boolean isFile, boolean createWhenNotExists) throws Exception {
        DocumentFile file = current.findFile(name);
        if (file != null) {
            if (file.isFile() == isFile) {
                return file;
            } else {
                throw new Exception("当前文件已存在，并且类型不同 源文件：" + file.isFile() + " 新文件：" + isFile);
            }
        } else {
            if (createWhenNotExists) {
                if (isFile) {
                    DocumentFile createdFile = current.createFile("*/*", name);
                    if (createdFile != null) {
                        return createdFile;
                    } else {
                        throw new Exception("创建文件失败");
                    }
                } else {
                    DocumentFile createdDirectory = current.createDirectory(name);
                    if (createdDirectory != null) {
                        return createdDirectory;
                    } else {
                        throw new Exception("创建文件夹失败");
                    }
                }
            } else {
                return null;
            }

        }
    }

    @Override
    public void changeToChild(@NonNull String name, boolean isFile, boolean createWhenNotExists) throws Exception {
        if (isFile()) {
            throw new Exception("当前是一个文件，无法向下操作");
        } else {
            DocumentFile newCurrent = getNewCurrent(name, isFile, createWhenNotExists);
            this.file = new File(file, name);
            this.current = newCurrent;
        }
    }

    protected DocumentLocalFileInstance getInstance() {
        return new DocumentLocalFileInstance(context, "", "", fileSystemRoot);
    }

    @Override
    public void list(List<FileItemModel> files, List<DirectoryItemModel> directories) {
        DocumentFile[] documentFiles = current.listFiles();
        for (DocumentFile documentFile : documentFiles) {
            if (needStop()) break;
            String documentFileName = documentFile.getName();
            assert documentFileName != null;
            String fullPath = new File(getPath(), documentFileName).getAbsolutePath();
            String detailString = getDetailString(documentFile);
            if (documentFile.isFile()) {
                FileInstanceUtility.addFile(files, fullPath, detailString, documentFile).setSize(documentFile.length());
            } else {
                FileInstanceUtility.adDirectory(directories, fullPath, detailString, documentFile);
            }
        }
    }

    InputStream getInputStream() throws FileNotFoundException {
        return context.getContentResolver().openInputStream(current.getUri());
    }

    OutputStream getOutputStream() throws FileNotFoundException {
        return context.getContentResolver().openOutputStream(current.getUri());
    }

    @Override
    public boolean deleteFileOrEmptyDirectory() {
        return current.delete();
    }

    @Override
    public LocalFileInstance toParent() throws Exception {
        File parentFile = file.getParentFile();
        if (parentFile == null) {
            throw new Exception("到头了，无法继续向上寻找");
        }
        DocumentLocalFileInstance instance = getInstance();
        DocumentFile currentParentFile = current.getParentFile();
        if (currentParentFile != null) {
            if (!currentParentFile.isFile()) {
                instance.file = parentFile;
                instance.current = currentParentFile;
                return instance;
            } else {
                throw new Exception("当前文件已存在，并且类型不同 源文件：" + currentParentFile.isFile());
            }
        } else {
            throw new Exception("查找parent DocumentFile失败");
        }

    }

    @Override
    public void changeToParent() throws Exception {
        File parentFile = file.getParentFile();
        if (parentFile == null) {
            throw new Exception("无法继续向上寻找");
        }
        DocumentFile documentFile = current.getParentFile();
        if (documentFile != null) {
            if (!documentFile.isFile()) {
                file = parentFile;
                current = documentFile;
            } else {
                throw new Exception("当前文件已存在，并且类型不同 源文件：" + documentFile.isFile());
            }
        } else {
            throw new Exception("查找parent DocumentFile失败");
        }

    }

    @Override
    public boolean isFile() {
        if (current == null) {
            Log.e(TAG, "isFile: path:" + getPath());
        }
        return current.isFile();
    }

    @Override
    public boolean exists() {
        if (current == null) {
            return false;
        }
        return current.exists();
    }

    @Override
    public boolean isDirectory() {
        if (current == null) {
            Log.e(TAG, "isDirectory: isDirectory:" + getPath());
        }
        return current.isDirectory();
    }

    @Override
    public void changeTo(@NonNull String path) {
        if (this.getPath().equals(path)) {
            return;
        }
        if (path.startsWith(prefix)) {
            file = new File(path);
            initDocumentFile();
        }
    }

    @Override
    public boolean rename(String newName) {
        return current.renameTo(newName);
    }

    @Override
    public BufferedOutputStream getBufferedOutputStream() throws FileNotFoundException {
        OutputStream outputStream = getOutputStream();
        return new BufferedOutputStream(outputStream);
    }

    @Override
    public BufferedInputStream getBufferedInputSteam() throws FileNotFoundException {
        return new BufferedInputStream(getInputStream());
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public BufferedReader getBufferedReader() throws FileNotFoundException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    @Override
    public BufferedWriter getBufferedWriter() throws FileNotFoundException {
        return new BufferedWriter(new OutputStreamWriter(getOutputStream()));
    }

    @Override
    public FileInputStream getFileInputStream() throws FileNotFoundException {
        ParcelFileDescriptor r = context.getContentResolver().openFileDescriptor(current.getUri(), "r");
        return new FileInputStream(r.getFileDescriptor());
    }

    @Override
    public FileOutputStream getFileOutputStream() throws FileNotFoundException {
        return new FileOutputStream(context.getContentResolver().openFileDescriptor(current.getUri(), "w").getFileDescriptor());
    }

    @Override
    public long getFileLength() {
        return current.length();
    }

    @Override
    public FileItemModel getFile() {
        return new FileItemModel(getName(), getPath(), false, current.lastModified(), FileUtility.getExtension(getName()));
    }

    @Override
    public DirectoryItemModel getDirectory() {
        return new DirectoryItemModel(getName(), getPath(), false, current.lastModified());
    }

    public static String getDetailString(DocumentFile file) {
        boolean w = file.canWrite();
        boolean r = file.canRead();
        return String.format(Locale.CHINA, "%c%c%c%c", (file.isFile() ? '-' : 'd'), (r ? 'r' : '-'), (w ? 'w' : '-'), '-');
    }

    public static DocumentLocalFileInstance getEmulated(Context context, String path) {
        return new DocumentLocalFileInstance(context, path, emulatedKey, FileInstanceFactory.publicFileSystemRoot);
    }

    public static DocumentLocalFileInstance getMounted(Context context, String path) {
        return new DocumentLocalFileInstance(context, path, mountedKey, FileInstanceFactory.publicFileSystemRoot);
    }
}
