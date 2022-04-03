package com.storyteller_f.file_system.operate;


import com.storyteller_f.file_system.instance.FileInstance;
import com.storyteller_f.file_system.message.Message;

public interface FileOperateListener {
    /**
     * 当一个文件处理完成
     *
     * @param type
     * @param message
     */
    void onOneFile(FileInstance fileInstance, int type, Message message);

    /**
     * 当一个文件夹处理完成
     *
     * @param type
     * @param message
     */
    void onOneDirectory(FileInstance fileInstance, int type, Message message);

    void onError(Message message, int type);
}
