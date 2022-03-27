package com.storyteller_f.file_system.operate;


import com.storyteller_f.file_system.instance.local.LocalFileInstance;

public abstract class FileOperateAim extends FileOperate {
    public abstract boolean doWork(LocalFileInstance dest);
}
