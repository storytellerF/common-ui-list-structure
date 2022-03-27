package com.storyteller_f.file_system.operate;

import com.storyteller_f.file_system.instance.FileInstance;
import com.storyteller_f.file_system.instance.local.LocalFileInstance;
import com.storyteller_f.file_system.message.Message;

public class LocalFileMoveOperate extends LocalFileCopyOperate {
    public LocalFileMoveOperate(LocalFileInstance localFileInstance, LocalFileInstance dest) {
        super(localFileInstance, dest);
    }

    @Override
    protected boolean afterCopy(LocalFileInstance fileInstance) {
        boolean b;
        try {
            b = fileInstance.deleteFileOrEmptyDirectory();
            if (fileOperateListener != null) {
                if (b) {
                    fileOperateListener.onOneFile(fileInstance.getPath(), fileInstance.getName(),
                            fileInstance.getFileLength(), LocalFileInstance.file_operate_type_move_delete,
                            new Message("复制文件之后删除文件成功" + fileInstance.getName()).add(fileInstance.getPath()));
                } else {
                    fileOperateListener.onError(
                            new Message("删除文件失败" + fileInstance.getName()).add(fileInstance.getPath()),
                            LocalFileInstance.file_operate_type_move_delete);
                }
            }
            if (!b) {
                return false;
            }
        } catch (Exception e) {
            if (fileOperateListener != null) {
                fileOperateListener.onError(
                        new Message("删除原文件异常" + fileInstance.getName()).add(e.getMessage()), LocalFileInstance.file_operate_type_move_delete);
            }
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected boolean afterCopyDirectory(FileInstance afterCopyDirectory) {
        boolean b;
        try {
            b = afterCopyDirectory.deleteFileOrEmptyDirectory();
            if (fileOperateListener != null) {
                if (b) {
                    fileOperateListener.onOneDirectory(afterCopyDirectory.getPath(), afterCopyDirectory.getName(),
                            LocalFileInstance.file_operate_type_move_delete, new Message("复制文件夹之后删除成功" + afterCopyDirectory.getName())
                                    .add(afterCopyDirectory.getPath()));
                } else {
                    fileOperateListener.onError(
                            new Message("删除文件夹失败" + afterCopyDirectory.getName()).add(afterCopyDirectory.getPath()),
                            LocalFileInstance.file_operate_type_move_delete);
                }
            }
            if (!b) {
                return false;
            }
        } catch (Exception e) {
            if (fileOperateListener != null) {
                fileOperateListener.onError(
                        new Message("删除原文件夹异常" + afterCopyDirectory.getName()).add(afterCopyDirectory.getPath()).add(e.getMessage()), FileInstance.file_operate_type_move_delete);
            }
            e.printStackTrace();
            return false;
        }

        return true;
    }
}
