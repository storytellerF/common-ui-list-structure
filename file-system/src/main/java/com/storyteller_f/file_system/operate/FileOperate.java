package com.storyteller_f.file_system.operate;

public abstract class FileOperate {
    FileOperateListener fileOperateListener;

    public void setFileOperateListener(FileOperateListener fileOperateListener) {
        this.fileOperateListener = fileOperateListener;
    }

    protected boolean isNeedStopByNext() {
        if (fileOperateListener != null) {
            return fileOperateListener.onThreadState();
        }
        return false;
    }

    public abstract boolean doWork() ;
}

