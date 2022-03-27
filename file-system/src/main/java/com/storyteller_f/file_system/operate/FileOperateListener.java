package com.storyteller_f.file_system.operate;


import com.storyteller_f.file_system.message.Message;

public interface FileOperateListener {

    void onOneFile(String path, String name, long size, int type, Message message);

    void onOneDirectory(String path, String name, int type, Message message);

    void onError(Message message, int type);

    /**
     *
     * @return 如果不能继续进行任务，返回true
     */
    boolean onThreadState();
}