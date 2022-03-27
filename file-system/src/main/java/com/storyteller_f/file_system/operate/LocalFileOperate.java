package com.storyteller_f.file_system.operate;


import com.storyteller_f.file_system.instance.local.LocalFileInstance;

public abstract class LocalFileOperate extends FileOperate {
    protected LocalFileInstance fileInstance;

    public LocalFileOperate(LocalFileInstance localFileInstance) {
        this.fileInstance = localFileInstance;
    }
}
