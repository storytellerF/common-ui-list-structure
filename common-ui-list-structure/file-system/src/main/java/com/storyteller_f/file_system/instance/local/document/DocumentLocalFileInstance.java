package com.storyteller_f.file_system.instance.local.document;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;


import com.storyteller_f.file_system.FileSystemUriSaver;
import com.storyteller_f.file_system.FileInstanceFactory;
import com.storyteller_f.file_system.Filter;
import com.storyteller_f.file_system.instance.local.LocalFileInstance;
import com.storyteller_f.file_system.model.DirectoryItemModel;
import com.storyteller_f.file_system.model.FileItemModel;
import com.storyteller_f.file_system.model.FileSystemItemModel;
import com.storyteller_f.file_system.model.FilesAndDirectories;

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
import java.util.ArrayList;
import java.util.Locale;

public abstract class DocumentLocalFileInstance extends LocalFileInstance {
    private static final String TAG = "DocumentLocalFileInstan";
    private static String sharedPreferenceName;
    private static String sharedPreferenceKey;
    protected DocumentFile current;
    /**
     * 用来标识对象所在区域，可能是外部，也可能是外部
     */
    String prefix;
    private String key;

    public DocumentLocalFileInstance(Filter filter, Context context, String path, String sharedPreferenceName, String sharedPreferenceKey) {
        super(context, filter, path);
        DocumentLocalFileInstance.sharedPreferenceName = sharedPreferenceName;
        DocumentLocalFileInstance.sharedPreferenceKey = sharedPreferenceKey;
        this.prefix = FileInstanceFactory.getPrefix(path, context);
    }

    public DocumentLocalFileInstance(Context context, String path, String sharedPreferenceName, String sharedPreferenceKey) {
        super(context, path);
        DocumentLocalFileInstance.sharedPreferenceName = sharedPreferenceName;
        DocumentLocalFileInstance.sharedPreferenceKey = sharedPreferenceKey;
        this.prefix = FileInstanceFactory.getPrefix(path, context);
    }

    public DocumentLocalFileInstance(Context context, String sharedPreferenceName, String sharedPreferenceKey) {
        super(context);
        DocumentLocalFileInstance.sharedPreferenceName = sharedPreferenceName;
        DocumentLocalFileInstance.sharedPreferenceKey = sharedPreferenceKey;
    }

    public static String getDetailString(DocumentFile file) {
        boolean w = file.canWrite();
        boolean r = file.canRead();
        return String.format(Locale.CHINA, "%c%c%c%c", (file.isFile() ? '-' : 'd'), (r ? 'r' : '-'), (w ? 'w' : '-'), '-');
    }

    @Override
    public void destroy() {
        super.destroy();
        prefix = null;
        current = null;
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

    /**
     * 获取指定目录的document file
     * @param destination         目标地址
     * @param createDirectoryFlag 决定在没有文件夹的时候是否创建文件夹
     * @return 返回目标文件
     */
    public DocumentFile getSpecialDocumentFile(String destination, boolean createDirectoryFlag) {
        if (!destination.startsWith(prefix)) {
            Log.e(TAG, "getCurrent: prefix 出错 " + prefix);
            return null;
        }
        String truePath = destination.substring(prefix.length());//获取 SD卡名称后面的路径
        Uri uri = FileSystemUriSaver.getInstance().getUri(key);
        if (truePath.isEmpty()) {
            return DocumentFile.fromTreeUri(context, uri);
        }
        DocumentFile rootFile = DocumentFile.fromTreeUri(context, uri);
        if (rootFile == null) {
            Log.e(TAG, "getCurrent: rootFile is null");
            return null;
        }
        String[] nameItemPath = truePath.split("/");
        //Log.i(TAG, "getCurrent: "+ Arrays.toString(nameItemPath));
        DocumentFile temp = rootFile;
        for (String name : nameItemPath) {
            if (needStop()) break;
            DocumentFile foundFile = temp.findFile(name);
            if (foundFile == null) {
                if (!createDirectoryFlag) {
                    //Log.d(TAG, "getCurrent:没有找到，而且根据设定，不用去创建");
                    return null;
                }
                DocumentFile created = temp.createDirectory(name);
                if (created == null) {
                    //Log.e(TAG, "getCurrent: 创建文件夹失败" + name);
                    return null;
                } else {
                    temp = created;
                    //Log.d(TAG, "getCurrent: 创建成功 " + name);
                }
            } else {
                temp = foundFile;
            }
        }
        return temp;
    }

    @Override
    public FileItemModel getFile() {
        return new FileItemModel(name, path, false, current.lastModified(), getExtension(name));
    }

    @Override
    public DirectoryItemModel getDirectory() {
        return new DirectoryItemModel(name, path, false, current.lastModified());
    }

    public void updateRootKey() {
        key = FileSystemUriSaver.getInstance().saveUri(sharedPreferenceName, sharedPreferenceKey, context);
        if (key == null) {
            Log.e(TAG, "updateRoot: 获取root失败:" + path + "，没有授予权限");
        }
    }

    /**
     * 初始化DocumentFile，初始化失败一般不影响获取目录,但是不可以对当前对象进行操作
     *
     * @return true 代表初始化成功
     */
    public boolean initDocumentFile() {
        if (key == null) {
            Log.e(TAG, "initCurrentFile: 获取root 失败" + path + "不会进行初始化");
            return false;
        }
        current = getSpecialDocumentFile(path, false);
        if (current == null)
            Log.e(TAG, "initCurrentFile: 初始化失败" + path);
        return current != null;
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
        DocumentFile destDirectory = getSpecialDocumentFile(path, true);
        if (destDirectory != null) {
            return initDocumentFile();
        }
        return false;
    }

    public boolean createFile() {
        File file = new File(getPath());
        String parent = file.getParent();
        if (parent == null) {
            return false;
        } else {
            DocumentFile parentDocumentFile = getSpecialDocumentFile(parent, true);
            if (parentDocumentFile != null) {
                DocumentFile file1 = parentDocumentFile.createFile("*/*", file.getName());
                if (file1 != null) {
                    return initDocumentFile();
                }
            }
        }
        return false;
    }

    @Override
    public LocalFileInstance toChild(String name, boolean isFile, boolean reCreate) throws Exception {
        if (!exists()) {
            Log.e(TAG, "toChild: 未经过初始化或者文件不存在：" + path);
            return null;
        }
        if (isFile()) {
            throw new Exception("当前是一个文件，无法向下操作");
        } else {
            DocumentLocalFileInstance instance = getInstance();
            instance.path = path + name;
            instance.name = name;
            if (!isFile) {
                instance.path += "/";
            }
            instance.current = getNewCurrent(name, isFile, reCreate);
            return instance;
        }
    }

    /**
     * @param name     名称
     * @param isFile   是否是文件
     * @param reCreate 是否创建
     * @return 如果查找不到，而且不用创建，返回null
     * @throws Exception 会出现无法预计的结果时，不允许再次继续
     */
    public DocumentFile getNewCurrent(String name, boolean isFile, boolean reCreate) throws Exception {
        DocumentFile file = current.findFile(name);
        if (file != null) {
            if (file.isFile() == isFile) {
                return file;
            } else {
                throw new Exception("当前文件已存在，并且类型不同 源文件：" + file.isFile() + " 新文件：" + isFile);
            }
        } else {
            if (reCreate) {
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
    public void changeToChild(String name, boolean isFile, boolean reCreate) throws Exception {
        if (isFile()) {
            throw new Exception("当前是一个文件，无法向下操作");
        } else {
            DocumentFile newCurrent = getNewCurrent(name, isFile, reCreate);
            this.path += name;
            this.name = name;
            if (!isFile) {
                this.path += "/";
            }
            this.current = newCurrent;
        }
    }

    protected abstract DocumentLocalFileInstance getInstance();

    @NonNull
    @Override
    public FilesAndDirectories listSafe() {
        return list();
    }

    @NonNull
    @Override
    public FilesAndDirectories list() {
        if (current == null) {
            return FilesAndDirectories.Companion.empty();
        }
        DocumentFile[] documentFiles = current.listFiles();
        ArrayList<FileItemModel> files = new ArrayList<>();
        ArrayList<DirectoryItemModel> directories = new ArrayList<>();
        for (DocumentFile documentFile : documentFiles) {
            if (needStop()) break;
            String documentFileName = documentFile.getName();
            if (documentFileName == null) {
                Log.e(TAG, "list: 出现一个错误");
                continue;
            }
            String abs = documentFile.getUri().getPath();
            String absp = new File(path, documentFileName).getAbsolutePath();
            Log.i(TAG, "list: directory abs:" + abs + " absp:" + absp);
            long time = documentFile.lastModified();
            FileSystemItemModel fileSystemItemModel;
            if (documentFile.isFile()) {
                String extension = getExtension(documentFileName);
                FileItemModel fileSystemItemModel1 = addFile(files, path, documentFileName.startsWith("."), documentFileName, absp, time, extension);
                fileSystemItemModel1.setSize(documentFile.length());
                fileSystemItemModel = fileSystemItemModel1;
            } else {
                fileSystemItemModel = addDirectory(directories, path, documentFileName.startsWith("."), documentFileName, absp, time);
            }
            if (fileSystemItemModel != null)
                fileSystemItemModel.setDetail(getDetailString(documentFile));
        }
        return new FilesAndDirectories(files, directories);
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
        File parentFile = new File(path).getParentFile();
        if (parentFile == null) {
            throw new Exception("到头了，无法继续向上寻找");
        }
        String p = parentFile.getAbsolutePath();
        DocumentLocalFileInstance instance = getInstance();
        DocumentFile currentParentFile = current.getParentFile();
        if (currentParentFile != null) {
            if (!currentParentFile.isFile()) {
                instance.path = p + "/";
                instance.name = parentFile.getName();
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
        File parentFile = new File(path).getParentFile();
        if (parentFile == null) {
            throw new Exception("无法继续向上寻找");
        }
        String p = parentFile.getAbsolutePath();
        DocumentFile documentFile = current.getParentFile();
        if (documentFile != null) {
            if (!documentFile.isFile()) {
                path = p + "/";
                name = documentFile.getName();
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
            Log.e(TAG, "isFile: path:" + path);
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
            Log.e(TAG, "isDirectory: isDirectory:" + path);
        }
        return current.isDirectory();
    }

    @Override
    public void changeTo(String path) {
        if (this.path.equals(path)) {
            return;
        }
        if (path.startsWith(prefix)) {
            this.path = path;
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
    public boolean isHide() {
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
}
