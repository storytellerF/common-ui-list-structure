package com.storyteller_f.file_system.instance.local.fake;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.storyteller_f.file_system.FileInstanceFactory;
import com.storyteller_f.file_system.Filter;
import com.storyteller_f.file_system.instance.local.LocalFileInstance;
import com.storyteller_f.file_system.model.DirectoryItemModel;
import com.storyteller_f.file_system.model.FileItemModel;
import com.storyteller_f.file_system.model.FilesAndDirectories;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 标识特殊目录/storage/emulated/
 */
public class EmulatedLocalFileInstance extends LocalFileInstance {
    private static final String TAG = "EmulatedLocalFileInstan";

    public EmulatedLocalFileInstance(Context context, Filter filter) {
        super(context, filter, FileInstanceFactory.emulatedRootPath);
    }

    public EmulatedLocalFileInstance(Context context) {
        super(context, FileInstanceFactory.emulatedRootPath);
    }

    @Override
    public FileItemModel getFile() {
        return null;
    }

    @Override
    public DirectoryItemModel getDirectory() {
        return new DirectoryItemModel("emulated", path, false, 0);
    }

    @Override
    public long getFileLength() {
        return 0;
    }

    @Override
    public BufferedOutputStream getBufferedOutputStream() {
        return null;
    }

    @Override
    public BufferedInputStream getBufferedInputSteam() {
        return null;
    }

    @Override
    public BufferedReader getBufferedReader() {
        return null;
    }

    @Override
    public BufferedWriter getBufferedWriter() {
        return null;
    }

    @Override
    public FileInputStream getFileInputStream() throws FileNotFoundException {
        return null;
    }

    @Override
    public FileOutputStream getFileOutputStream() throws FileNotFoundException {
        return null;
    }

    @NonNull
    @Override
    public FilesAndDirectories listSafe() {
        return list();
    }

    @NonNull
    @Override
    public FilesAndDirectories list() {
        List<DirectoryItemModel> directoryItemModels = new ArrayList<>();
        directoryItemModels.add(new DirectoryItemModel("0", path + "/0", false, new File(FileInstanceFactory.rootUserEmulatedPath).lastModified()));
        return new FilesAndDirectories(new ArrayList<>(), directoryItemModels);
    }

    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public boolean deleteFileOrEmptyDirectory() {
        return false;
    }

    @Override
    public boolean rename(String newName) {
        return false;
    }

    @Override
    public LocalFileInstance toParent() {
        return null;
    }

    @Override
    public void changeToParent() {

    }

    @Override
    public long getDirectorySize() {
        return 0;
    }

    @Override
    public boolean createFile() {
        return false;
    }

    @Override
    public boolean isHidden() {
        return false;
    }

    @Override
    public boolean createDirectory() {
        return false;
    }

    @Override
    public LocalFileInstance toChild(String name, boolean isFile, boolean reCreate) {
        return null;
    }

    @Override
    public void changeToChild(String name, boolean isFile, boolean reCreate) {

    }

    @Override
    public void changeTo(String path) {

    }

    @Override
    public String getParent() {
        return null;
    }
}
