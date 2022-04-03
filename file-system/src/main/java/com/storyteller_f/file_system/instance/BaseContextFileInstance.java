package com.storyteller_f.file_system.instance;

import android.content.Context;

import com.storyteller_f.file_system.Filter;

public abstract class BaseContextFileInstance extends FileInstance {
    protected Context context;

    /**
     * @param filter 遍历文件夹用的
     * @param path   路径
     */
    public BaseContextFileInstance(Context context, Filter filter, String path) {
        super(filter, path);
        this.context = context;
    }

    public BaseContextFileInstance(Context context, String path) {
        super(path);
        this.context = context;
    }

    public BaseContextFileInstance(Context context) {
        this.context = context;
    }
}
