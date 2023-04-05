package com.storyteller_f.file_system.instance.local.fake;

import android.content.Context;

import androidx.annotation.NonNull;

import com.storyteller_f.file_system.FileInstanceFactory;
import com.storyteller_f.file_system.instance.local.LocalFileInstance;
import com.storyteller_f.file_system.model.DirectoryItemModel;
import com.storyteller_f.file_system.model.FileItemModel;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;

/**
 * 标识特殊目录/storage/emulated/
 */
public class EmulatedLocalFileInstance extends LocalFileInstance {

    public EmulatedLocalFileInstance(Context context) {
        super(context, FileInstanceFactory.emulatedRootPath, FileInstanceFactory.publicFileSystemRoot);
    }

    @Override
    public FileItemModel getFile() {
        return null;
    }

    @Override
    public DirectoryItemModel getDirectory() {
        return new DirectoryItemModel("emulated", getPath(), false, 0, false);
    }

    @Override
    public long getFileLength() {
        return 0;
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

    @Override
    public void listInternal(@NonNull List<FileItemModel> fileItems, @NonNull List<DirectoryItemModel> directoryItems) {
        directoryItems.add(new DirectoryItemModel("0", getPath() + "/0", false, new File(FileInstanceFactory.rootUserEmulatedPath).lastModified(), false));
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
    public LocalFileInstance toChild(@NonNull String name, boolean isFile, boolean createWhenNotExists) {
        return null;
    }

    @Override
    public void changeToChild(@NonNull String name, boolean isFile, boolean createWhenNotExists) {

    }

    @Override
    public void changeTo(@NonNull String path) {

    }

    @Override
    public String getParent() {
        return null;
    }
}
