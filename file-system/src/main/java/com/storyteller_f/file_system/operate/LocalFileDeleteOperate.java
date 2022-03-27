package com.storyteller_f.file_system.operate;


import com.storyteller_f.file_system.instance.FileInstance;
import com.storyteller_f.file_system.instance.local.LocalFileInstance;
import com.storyteller_f.file_system.message.Message;
import com.storyteller_f.file_system.model.DirectoryItemModel;
import com.storyteller_f.file_system.model.FileItemModel;
import com.storyteller_f.file_system.model.FilesAndDirectories;

import java.util.List;

public class LocalFileDeleteOperate extends LocalFileOperate {
    public LocalFileDeleteOperate(LocalFileInstance localFileInstance) {
        super(localFileInstance);
    }

    @Override
    public boolean doWork() {
        try {
            if (fileInstance.isFile()) {
                return deleteFile(fileInstance);
            } else {
                return deleteDirectory(fileInstance);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (fileOperateListener != null) {
                fileOperateListener.onError(new Message("出现异常" + e.getMessage()), FileInstance.file_operate_type_delete);
            }
        }
        return false;
    }

    /**
     * 两个参数应该是一体的
     *
     * @param current 目录实体
     * @return 整体任务是否完成，包括子文件，子文件夹，当前文件夹
     * @throws Exception 删除文件时，会有异常
     */
    private boolean deleteDirectory(LocalFileInstance current) throws Exception {
        FilesAndDirectories filesAndDirectories = current.listSafe();
        List<FileItemModel> files = filesAndDirectories.getFiles();
        for (FileItemModel f : files) {
            if (isNeedStopByNext()) break;
            LocalFileInstance instance = (LocalFileInstance) current.toChild(f.getName(), true, false);
            boolean delete = instance.deleteFileOrEmptyDirectory();
            if (fileOperateListener != null) {
                if (delete) {
                    fileOperateListener.onOneFile(fileInstance.getPath(), fileInstance.getName(),
                            fileInstance.getFileLength(), LocalFileInstance.file_operate_type_move_delete,
                            new Message("删除一个文件成功" + fileInstance.getName()).add(fileInstance.getPath()));
                } else {
                    fileOperateListener.onError(
                            new Message("删除文件失败" + fileInstance.getName()).add(fileInstance.getPath()),
                            LocalFileInstance.file_operate_type_move_delete);
                }
            }
            if (!delete) {//因为文件删除失败，整体的任务都结束
                return false;
            }
        }
        List<DirectoryItemModel> directories = filesAndDirectories.getDirectories();
        for (DirectoryItemModel directory : directories) {
            if (isNeedStopByNext()) break;
            boolean b = deleteDirectory((LocalFileInstance) current.toChild(directory.getName(), false, false));
            if (fileOperateListener != null) {
                if (b) {
                    fileOperateListener.onOneDirectory(directory.getAbsolutePath(), directory.getName(),
                            LocalFileInstance.file_operate_type_move_delete,
                            new Message("删除一个文件夹成功" + directory.getName())
                                    .add(directory.getAbsolutePath()));
                } else {
                    fileOperateListener.onError(
                            new Message("删除文件夹失败" + directory.getName())
                                    .add(directory.getAbsolutePath()),
                            LocalFileInstance.file_operate_type_move_delete);
                }
            }
            if (!b) {
                return false;
            }
        }

        //到此处子目录和子文件删除成功，开始删除当前文件夹本身
        boolean b = current.deleteFileOrEmptyDirectory();
        if (fileOperateListener != null) {
            if (b) {
                fileOperateListener.onOneDirectory(current.getPath(), current.getName(),
                        FileInstance.file_operate_type_delete,
                        new Message("删除文件夹成功（文件夹本身）" + current.getName()).add(current.getPath()));
            } else {
                fileOperateListener.onError(
                        new Message("删除文件夹失败（文件夹本身）" + current.getName()).add(current.getParent()), FileInstance.file_operate_type_delete);
            }
        }
        return b;
    }

    private boolean deleteFile(LocalFileInstance current) {
        try {
            boolean b = current.deleteFileOrEmptyDirectory();
            if (b) {
                fileOperateListener.onOneDirectory(current.getPath(), current.getName(),
                        LocalFileInstance.file_operate_type_move_delete,
                        new Message("删除一个文件成功" + current.getName()).add(current.getPath()));
            } else {
                fileOperateListener.onError(
                        new Message("删除文件夹失败" + current.getName()).add(current.getPath()),
                        LocalFileInstance.file_operate_type_move_delete);
            }
            if (!b) {
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (fileOperateListener != null) {
                fileOperateListener.onError(
                        new Message("删除文件时出现异常：" + current.getName())
                                .add(e.getMessage()), FileInstance.file_operate_type_delete);
            }
        }
        return false;
    }
}
