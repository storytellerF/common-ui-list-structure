package com.storyteller_f.file_system.model;

public class SimpleFileItemModel extends FileSystemItemModelLite {
    public long length;
    public SimpleFileItemModel(String name, String absolutePath,long length) {
        super(name, absolutePath);
        this.length=length;
    }
}
