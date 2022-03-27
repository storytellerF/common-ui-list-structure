package com.storyteller_f.file_system.model;

public class SimpleDirectoryItemModel extends FileSystemItemModelLite{
    public long fileCount;
    public long folderCount;
    public SimpleDirectoryItemModel(String name, String absolutePath) {
        super(name, absolutePath);
    }
}
