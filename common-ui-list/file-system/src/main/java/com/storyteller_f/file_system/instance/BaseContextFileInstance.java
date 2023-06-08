package com.storyteller_f.file_system.instance;

import android.content.Context;

public abstract class BaseContextFileInstance extends FileInstance {
    protected Context context;

    /**
     * @param path   路径
     */
    public BaseContextFileInstance(Context context, String path, String fileSystemRoot) {
        super(path, fileSystemRoot);
        this.context = context;
    }
}
