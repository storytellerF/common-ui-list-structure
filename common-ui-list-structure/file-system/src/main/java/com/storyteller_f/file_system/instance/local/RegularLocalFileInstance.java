package com.storyteller_f.file_system.instance.local;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.storyteller_f.file_system.Filter;
import com.storyteller_f.file_system.model.DirectoryItemModel;
import com.storyteller_f.file_system.model.FileItemModel;
import com.storyteller_f.file_system.model.FileSystemItemModel;
import com.storyteller_f.file_system.model.FilesAndDirectories;
import com.storyteller_f.file_system.util.FileUtility;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public FileItemModel getFile() {
        return new FileItemModel(name, path, file.isHidden(), file.lastModified(), getExtension(name));
    }

    @Override
    public DirectoryItemModel getDirectory() {
        return new DirectoryItemModel(name, path, file.isHidden(), file.lastModified());
    }

    @Override
    public long getFileLength() {
        return file.length();
    }

    @Override
    public boolean createFile() throws IOException {
        return new File(getPath()).createNewFile();
    }

    @Override
    public boolean isHidden() {
        return file.isHidden();
    }

    @Override
    public boolean createDirectory() {
        return new File(getPath()).mkdirs();
    }

    @Override
    public LocalFileInstance toChild(String name, boolean isFile, boolean reCreate) throws Exception {
        File file = new File(path, name);
        RegularLocalFileInstance internalFileInstance = new RegularLocalFileInstance(context, filter, file.getAbsolutePath());
        if (exists()) {
            if (isFile()) {
                throw new Exception("当前是一个文件，无法向下操作");
            } else {
                //检查目标文件是否存在
                if (file.exists()) {
                    if (file.isFile() == isFile) {
                        return internalFileInstance;
                    } else {
                        throw new Exception("当前文件已经存在，并且类型不符合：" + file.isFile() + " " + isFile);
                    }
                } else {
                    if (reCreate) if (isFile) {
                        boolean newFile = file.createNewFile();
                        if (newFile) {
                            return internalFileInstance;
                        } else {
                            throw new Exception("新建文件失败");
                        }
                    } else {
                        boolean mkdirs = file.mkdirs();
                        if (mkdirs) {
                            return internalFileInstance;
                        } else {
                            throw new Exception("新建文件失败");
                        }
                    }
                }
            }
        }
        throw new Exception("当前文件或者文件夹不存在");
    }

    @Override
    public void changeToChild(String name, boolean isFile, boolean reCreate) throws Exception {
        Log.d(TAG, "changeToChild() called with: name = [" + name + "], isFile = [" + isFile + "]");
        File file = new File(path, name);
        if (exists()) {
            if (isFile()) {
                throw new Exception("当前是一个文件，无法向下操作");
            } else {
                //检查目标文件是否存在
                if (file.exists()) {
                    if (file.isFile() == isFile) {
                        this.path = this.path + name;
                        if (!isFile) {
                            this.path += "/";
                        }
                        this.file = file;
                        return;
                    } else {
                        throw new Exception("当前文件已经存在，并且类型不符合：" + file.isFile() + " " + isFile);
                    }
                } else {
                    if (reCreate) if (isFile) {
                        boolean newFile = file.createNewFile();
                        if (newFile) {
                            this.path = this.path + name;
                            this.file = file;
                            return;
                        } else {
                            throw new Exception("新建文件失败");
                        }
                    } else {
                        boolean mkdirs = file.mkdirs();
                        if (mkdirs) {
                            this.file = file;
                            this.path = this.path + name + "/";
                            return;
                        } else {
                            throw new Exception("新建文件失败");
                        }
                    }
                }
            }
        }
        throw new Exception("当前文件或者文件夹不存在。path:" + path);
    }

    @Override
    public void changeTo(String path) {
        if (this.path.equals(path)) {
            return;
        }
        this.path = path;
        this.file = new File(path);
    }

    @Override
    public String getParent() {
        return file.getParent();
    }

    /**
     * 添加普通文件，判断过滤监听事件
     *
     * @param files  填充目的地
     * @param parent 父文件夹
     * @param sub    当前目录的子文件
     */
    protected FileSystemItemModel addFileBySystemFileObject(ArrayList<FileItemModel> files, File parent, File sub) {
        return addFile(files, parent.getAbsolutePath(), sub.isHidden(), sub.getName(), sub.getAbsolutePath(), sub.lastModified(), getExtension(sub.getName()));
    }

    @Override
    public BufferedOutputStream getBufferedOutputStream() throws FileNotFoundException {
        return new BufferedOutputStream(getFileOutputStream());
    }

    @Override
    public BufferedInputStream getBufferedInputSteam() throws FileNotFoundException {
        return new BufferedInputStream(new FileInputStream(path));
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

    @WorkerThread
    private FilesAndDirectories getFilesAndDirectoriesByListFiles() {
        FilesAndDirectories filesAndDirectories = listSafe();
        List<FileItemModel> f = filesAndDirectories.getFiles();
        List<DirectoryItemModel> d = filesAndDirectories.getDirectories();
        for (FileSystemItemModel fileItemModel : f) {
            if (needStop()) break;
            String detailString;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                detailString = FileUtility.getPermissionString(fileItemModel);
            } else {
                detailString = FileUtility.getPermissionStringByFile(fileItemModel);
            }
            fileItemModel.setPermissions(detailString);
        }
        for (FileSystemItemModel fileItemModel : d) {
            if (needStop()) break;
            String detailString;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                detailString = FileUtility.getPermissionString(fileItemModel);
            } else {
                detailString = FileUtility.getPermissionStringByFile(fileItemModel);
            }

            fileItemModel.setPermissions(detailString);
        }
        return filesAndDirectories;
    }

    @WorkerThread
    private FilesAndDirectories getFilesAndDirectoriesByCommand(ArrayList<FileItemModel> files, ArrayList<DirectoryItemModel> directories) {
        ProcessBuilder processBuilder = new ProcessBuilder("ls", "-Al").directory(new File(path));
        Process process = null;
        BufferedReader error = null;
        BufferedReader bufferedReader = null;
        try {
            process = processBuilder.start();
            error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            if (bufferedReader.readLine() != null) {
                String temp;
                while ((temp = bufferedReader.readLine()) != null) {
                    if (needStop()) break;
                    turnCmdLineToFileObject(files, directories, temp);
                }
                return new FilesAndDirectories(files, directories);
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            close(error);
            close(bufferedReader);
            if (process != null) {
                process.destroy();
            }
        }
    }

    private void close(BufferedReader error) {
        try {
            if (error != null) {
                error.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void turnCmdLineToFileObject(ArrayList<FileItemModel> files, ArrayList<DirectoryItemModel> directories, String temp) {
        int index = 0;
        int space_count = 0;
        Pattern pattern = Pattern.compile("\\s+");
        Matcher matcher = pattern.matcher(temp);
        while (matcher.find()) {
            if (needStop()) break;
            space_count++;
            if (space_count == 7) {
                index = matcher.start();
                break;
            }
        }

        String detail = temp.substring(0, index);
        String name = temp.substring(index + 1);
        File childFile = new File(path, name);
        boolean hidden = childFile.isHidden();
        if (childFile.isFile()) {
            addDetailFileByCmd(files, path, childFile, detail, hidden);
        } else if (childFile.isDirectory()) {
            addDetailDirectoryByCmd(directories, path, childFile, detail, hidden);
        }
    }


    @NonNull
    @Override
    @WorkerThread
    public FilesAndDirectories list() {
        ArrayList<FileItemModel> files = new ArrayList<>();
        ArrayList<DirectoryItemModel> directories = new ArrayList<>();
        File[] listFiles = file.listFiles();//获取子文件

        if (listFiles != null) {
            for (File childFile : listFiles) {
                FileSystemItemModel fileSystemItemModel = null;
                // 判断是否为文件夹
                if (childFile.isDirectory()) {
                    fileSystemItemModel = addDirectoryByFileObject(directories, file, childFile);
                    if (!fileSystemItemModel.getFullPath().endsWith("/")) {
                        Log.e(TAG, "list: 最后一个不是slash");
                    }
                } else if (childFile.isFile()) {
                    fileSystemItemModel = addFileBySystemFileObject(files, file, childFile);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        BasicFileAttributes basicFileAttributes = Files.readAttributes(childFile.toPath(), BasicFileAttributes.class);
                        if (fileSystemItemModel != null) {
                            fileSystemItemModel.setCreatedTime(basicFileAttributes.creationTime().toMillis());
                            fileSystemItemModel.setLastAccessTime(basicFileAttributes.lastAccessTime().toMillis());
                        }
                    } catch (IOException e) {
                        Log.w(TAG, "list: 获取BasicFileAttribute失败" + childFile.getAbsolutePath());
                    }
                }
            }
        } else {
            return null;
        }
        return new FilesAndDirectories(files, directories);

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

    private FileSystemItemModel addDirectoryByFileObject(ArrayList<DirectoryItemModel> directories, File file, File childFile) {
        return addDirectory(directories, file.getAbsolutePath(), childFile.isHidden(), childFile.getName(), childFile.getAbsolutePath(), childFile.lastModified());
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
        return new RegularLocalFileInstance(context, filter, new File(path).getParent());
    }

    @Override
    public void changeToParent() {
        File parentFile = new File(path).getParentFile();
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
