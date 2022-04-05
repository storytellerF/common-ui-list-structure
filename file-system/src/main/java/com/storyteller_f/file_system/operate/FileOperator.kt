package com.storyteller_f.file_system.operate;

import android.content.Context;

import androidx.annotation.Nullable;

import com.storyteller_f.file_system.instance.FileInstance;
import com.storyteller_f.multi_core.StoppableTask;

public abstract class FileOperator {
    @Nullable
    FileOperateListener fileOperateListener;
    protected StoppableTask task;
    protected FileInstance fileInstance;
    protected Context context;

    public FileOperator(StoppableTask task, FileInstance fileInstance, Context context) {
        this.task = task;
        this.fileInstance = fileInstance;
        this.context = context;
    }

    public abstract boolean doWork();
}

