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

        String permissions = temp.substring(0, index);
        String name = temp.substring(index + 1);
        File childFile = new File(path, name);
        boolean hidden = childFile.isHidden();
        if (childFile.isFile()) {
            addFile(files, path, childFile, permissions);
        } else if (childFile.isDirectory()) {
            addDirectory(directories, path, childFile, permissions);
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
                String permissions;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    permissions = FileUtility.getPermissionString(childFile.isFile(), childFile.getAbsolutePath());
                } else {
                    permissions = FileUtility.getPermissionStringByFile(childFile.isFile(), childFile.getAbsolutePath());
                }
                FileSystemItemModel fileSystemItemModel = null;
                // 判断是否为文件夹
                if (childFile.isDirectory()) {
                    fileSystemItemModel = addDirectory(directories, file.getAbsolutePath(), childFile, permissions);
                    if (!fileSystemItemModel.getFullPath().endsWith("/")) {
                        Log.e(TAG, "list: 最后一个不是slash");
                    }
                } else if (childFile.isFile()) {
                    fileSystemItemModel = addFile(files, file.getAbsolutePath(), childFile, permissions);
                }
                editAccessTime(childFile, fileSystemItemModel);
            }
        } else {
            return FilesAndDirectories.Companion.empty();
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
