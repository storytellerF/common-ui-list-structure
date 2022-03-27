package com.storyteller_f.file_system.instance.local;

import android.content.Context;
import android.util.Log;

import com.storyteller_f.file_system.Filter;
import com.storyteller_f.file_system.model.DirectoryItemModel;
import com.storyteller_f.file_system.model.FileItemModel;
import com.storyteller_f.file_system.model.FilesAndDirectories;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;

public class EmulatedLocalFileInstance extends LocalFileInstance {
    private static final String TAG = "EmulatedLocalFileInstan";

    public EmulatedLocalFileInstance(Context context, Filter filter) {
        super(context, filter, "/storage/emulated/");
    }

    public EmulatedLocalFileInstance(Context context) {
        super(context, "/storage/emulated/");
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
    public FilesAndDirectories listSafe() {
        return list();
    }

    @Override
    public FilesAndDirectories list() {
        Log.d(TAG, "list() called");
        List<DirectoryItemModel> directoryItemModels = new ArrayList<>();
        directoryItemModels.add(new DirectoryItemModel("0", path + "0/", false, 0));
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
    public boolean isHide() {
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
