package com.storyteller_f.file_system.instance.local;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import com.storyteller_f.file_system.FileSystemUriSaver;
import com.storyteller_f.file_system.instance.BaseContextFileInstance;
import com.storyteller_f.file_system.instance.Create;
import com.storyteller_f.file_system.instance.FileCreatePolicy;
import com.storyteller_f.file_system.instance.NotCreate;
import com.storyteller_f.file_system.model.DirectoryItemModel;
import com.storyteller_f.file_system.model.FileItemModel;
import com.storyteller_f.file_system.util.FileInstanceUtility;
import com.storyteller_f.file_system.util.FileUtility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import kotlin.Pair;

public class DocumentLocalFileInstance extends BaseContextFileInstance {
    private static final String TAG = "DocumentLocalFileInstan";
    public static final String EXTERNAL_STORAGE_DOCUMENTS = "com.android.externalstorage.documents";

    protected DocumentFile current;

    /**
     * 用来标识对象所在区域，可能是外部，也可能是内部
     * 比如/storage/XXXX-XXXX
     */
    String prefix;
    /**
     * 一般是authority
     */
    private String preferenceKey;

    private String pathRelativeRoot;

    public DocumentLocalFileInstance(Context context, Uri uri, String preferenceKey, String prefix) {
        super(context, uri);
        this.prefix = prefix;
        assertInit();
        this.preferenceKey = preferenceKey;
        current = getDocumentFile();
    }

    private DocumentLocalFileInstance(Context context, Uri uri, String prefix) {
        super(context, uri);
        this.prefix = prefix;
        assertInit();
    }

    private void assertInit() {
        assert getPath().startsWith(prefix);
        if (Objects.equals(getPath(), prefix)) {
            pathRelativeRoot = "/";
        } else {
            pathRelativeRoot = getPath().substring(prefix.length());
        }
        assert pathRelativeRoot.startsWith("/");

    }

    /**
     * 初始化DocumentFile，初始化失败一般不影响获取目录,但是不可以对当前对象进行操作
     */
    public DocumentFile getDocumentFile() {
        return getDocumentFile(NotCreate.INSTANCE);
    }

    /**
     * 获取指定目录的document file
     *
     * @return 返回目标文件
     */
    private DocumentFile getDocumentFile(FileCreatePolicy policy) {
        //此uri 是当前前缀下的根目录uri。fileInstance 的uri 是fileSystem 使用的uri。
        Uri rootUri = FileSystemUriSaver.getInstance().savedUri(context, preferenceKey);
        if (rootUri == null) {
            Log.e(TAG, "getDocumentFile: rootUri is null");
            return null;
        }
        DocumentFile rootFile = DocumentFile.fromTreeUri(context, rootUri);
        if (rootFile == null) {
            Log.e(TAG, "getDocumentFile: fromTreeUri is null");
            return null;
        }
        if (!rootFile.canRead()) {
            Log.e(TAG, "getDocumentFile: 权限过期, 不可读写 " + getPath() + " prefix: " + prefix);
            return null;
        }

        if (pathRelativeRoot.equals("/")) return rootFile;
        String[] nameItemPath = pathRelativeRoot.substring(1).split("/");
        String[] paths;
        boolean endElementIsFileName = policy instanceof Create && ((Create) policy).isFile();
        if (endElementIsFileName) {
            paths = Arrays.copyOf(nameItemPath, nameItemPath.length - 1);
        } else paths = nameItemPath;

        DocumentFile temp = rootFile;
        for (String name : paths) {
            if (needStop()) break;
            DocumentFile foundFile = temp.findFile(name);
            if (foundFile == null) {
                if (policy instanceof NotCreate) {
                    Log.e(TAG, "getDocumentFile: 文件找不到" + getPath() + " prefix: " + prefix);
                    return null;
                }
                DocumentFile created = temp.createDirectory(name);
                if (created == null) {
                    Log.e(TAG, "getDocumentFile: 文件创建失败" + getPath() + " prefix: " + prefix);
                    return null;
                } else temp = created;
            } else temp = foundFile;
        }

        //find file
        if (endElementIsFileName) {
            String fileName = nameItemPath[nameItemPath.length - 1];
            DocumentFile file = temp.findFile(fileName);
            if (file == null) {
                return temp.createFile("*/*", fileName);
            } else return file;
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
        if (current != null) return true;
        DocumentFile created = getDocumentFile(new Create(false));
        if (created != null) {
            current = created;
            return true;
        }
        return false;
    }

    public boolean createFile() {
        if (current != null) return true;
        DocumentFile created = getDocumentFile(new Create(true));
        if (created != null) {
            current = created;
            return true;
        }
        return false;
    }

    @Override
    public BaseContextFileInstance toChild(@NonNull String name, FileCreatePolicy policy) throws Exception {
        if (!exists()) {
            Log.e(TAG, "toChild: 未经过初始化或者文件不存在：" + getPath());
            return null;
        }
        if (isFile()) {
            throw new Exception("当前是一个文件，无法向下操作");
        } else {
            Uri build = uri.buildUpon().path(new File(getPath(), name).getAbsolutePath()).build();
            DocumentLocalFileInstance instance = new DocumentLocalFileInstance(context, build, prefix);
            instance.preferenceKey = preferenceKey;
            instance.current = getChild(name, policy);
            return instance;
        }
    }

    /**
     * @param name 名称
     * @return 如果查找不到，而且不用创建，返回null
     * @throws Exception 会出现无法预计的结果时，不允许再次继续
     */
    public DocumentFile getChild(String name, FileCreatePolicy policy) throws Exception {
        DocumentFile file = current.findFile(name);
        if (file != null) {
            return file;
        } else if (!(policy instanceof Create)) {
            return null;
        } else if (((Create) policy).isFile()) {
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
    }

    @Override
    public void changeToChild(@NonNull String name, FileCreatePolicy policy) throws Exception {
        if (isFile()) {
            throw new Exception("当前是一个文件，无法向下操作");
        } else {
            this.current = getChild(name, policy);
        }
    }

    @Override
    public void listInternal(@NonNull List<FileItemModel> files, @NonNull List<DirectoryItemModel> directories) throws Exception {
        if (current == null) {
            current = getDocumentFile();
        }
        DocumentFile c = current;
        if (c == null) throw new Exception("no permission");
        DocumentFile[] documentFiles = c.listFiles();
        for (DocumentFile documentFile : documentFiles) {
            if (needStop()) break;
            String documentFileName = documentFile.getName();
            assert documentFileName != null;
            String detailString = FileUtility.getPermissions(documentFile);
            Pair<File, Uri> t = child(documentFile, documentFileName);
            if (documentFile.isFile()) {
                FileInstanceUtility.addFile(files, t, detailString).setSize(documentFile.length());
            } else {
                FileInstanceUtility.addDirectory(directories, t, detailString);
            }
        }
    }

    @NonNull
    private Pair<File, Uri> child(DocumentFile documentFile, String documentFileName) {
        Pair<File, Uri> child = child(documentFileName);
        return new Pair<>(new File(child.getFirst().getAbsolutePath()) {
            @Override
            public long lastModified() {
                return documentFile.lastModified();
            }

            @Override
            public long length() {
                return documentFile.length();
            }
        }, child.getSecond());
    }

    @Override
    public boolean deleteFileOrEmptyDirectory() {
        return current.delete();
    }

    @Override
    public BaseContextFileInstance toParent() throws Exception {
        File parentFile = new File(getPath()).getParentFile();
        if (parentFile == null) {
            throw new Exception("到头了，无法继续向上寻找");
        }
        DocumentFile currentParentFile = current.getParentFile();
        if (currentParentFile == null) {
            throw new Exception("查找parent DocumentFile失败");
        } else if (!currentParentFile.isFile()) {
            Uri parent = uri.buildUpon().path(parentFile.getAbsolutePath()).build();
            DocumentLocalFileInstance instance = new DocumentLocalFileInstance(context, parent, prefix);
            instance.preferenceKey = preferenceKey;
            instance.current = currentParentFile;
            return instance;
        } else {
            throw new Exception("当前文件已存在，并且类型不同 源文件：" + currentParentFile.isFile());
        }

    }

    @Override
    public void changeToParent() throws Exception {
        File parentFile = new File(getPath()).getParentFile();
        if (parentFile == null) {
            throw new Exception("无法继续向上寻找");
        }
        DocumentFile documentFile = current.getParentFile();
        if (documentFile == null) {
            throw new Exception("查找parent DocumentFile失败");
        } else if (!documentFile.isFile()) {
            current = documentFile;
        } else {
            throw new Exception("当前文件已存在，并且类型不同 源文件：" + documentFile.isFile());
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
            current = getDocumentFile();
        }
    }

    @Override
    public boolean rename(String newName) {
        return current.renameTo(newName);
    }

    @Override
    public boolean isHidden() {
        return false;
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
        return new FileItemModel(getName(), uri, false, current.lastModified(), false, FileUtility.getExtension(getName()));
    }

    @Override
    public DirectoryItemModel getDirectory() {
        return new DirectoryItemModel(getName(), uri, false, current.lastModified(), false);
    }

    public static DocumentLocalFileInstance getEmulated(Context context, Uri uri, String prefix) {
        return new DocumentLocalFileInstance(context, uri, EXTERNAL_STORAGE_DOCUMENTS, prefix);
    }

    /**
     * sd 卡使用特殊的preferenceKey，就是路径
     */
    public static DocumentLocalFileInstance getMounted(Context context, Uri uri, String prefix) {
        //fixme sdCard 与emulated authority 相同，只是rootId 不同
        return new DocumentLocalFileInstance(context, uri, prefix, prefix);
    }
}
