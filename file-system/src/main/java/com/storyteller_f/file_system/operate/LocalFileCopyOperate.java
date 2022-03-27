package com.storyteller_f.file_system.operate;


import com.storyteller_f.file_system.instance.FileInstance;
import com.storyteller_f.file_system.instance.local.LocalFileInstance;
import com.storyteller_f.file_system.message.Message;
import com.storyteller_f.file_system.model.DirectoryItemModel;
import com.storyteller_f.file_system.model.FileItemModel;
import com.storyteller_f.file_system.model.FilesAndDirectories;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.util.List;

public class LocalFileCopyOperate extends LocalFileOperate {
    private final LocalFileInstance destination;

    public LocalFileCopyOperate(LocalFileInstance localFileInstance, LocalFileInstance destination) {
        super(localFileInstance);
        this.destination = destination;
    }

    /**
     * @param fileInstance 需要被移动的文件
     * @param to           代表的依然是路径，而不是现成的文件
     */
    boolean copyFileTo(LocalFileInstance fileInstance, LocalFileInstance to) {
        BufferedInputStream bufferedInputSteam = null;
        BufferedOutputStream bufferedOutputStream = null;
        String name = fileInstance.getName();
        try {
            if (!to.exists()) {
                boolean directory = to.createDirectory();
                if (!directory) {
                    if (fileOperateListener != null) {
                        fileOperateListener.onError(new Message("创建目录失败" + to.getName()).add(to.getPath()), 0);
                    }
                    return false;
                }
            }
            FileInstance destFileInstance = to.toChild(name, true, true);
            bufferedInputSteam = fileInstance.getBufferedInputSteam();
            bufferedOutputStream = destFileInstance.getBufferedOutputStream();
            byte[] bytes = new byte[1024];
            int len;
            while ((len = bufferedInputSteam.read(bytes)) != -1) {
                if (isNeedStopByNext()) break;
                bufferedOutputStream.write(bytes, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (fileOperateListener != null) {
                fileOperateListener.onError(new Message("复制文件时出现异常" + fileInstance.getName())
                                .add(fileInstance.getPath()).add(e.getMessage()),
                        LocalFileInstance.file_operate_type_copy);
            }
            return false;
        } finally {
            closeStream(bufferedInputSteam);
            flushStream(bufferedOutputStream);
            closeStream(bufferedOutputStream);
        }
        if (fileOperateListener != null) {
            fileOperateListener.onOneFile(fileInstance.getPath(), fileInstance.getName(), fileInstance.getFileLength(), LocalFileInstance.file_operate_type_copy,
                    new Message("复制一个文件成功"+fileInstance.getName()).add(fileInstance.getPath()));
        }
        return afterCopy(fileInstance);
    }

    private void closeStream(Closeable bufferedOutputStream) {
        try {
            if (bufferedOutputStream != null) {
                bufferedOutputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void flushStream(Flushable flushable) {
        try {
            if (flushable != null) {
                flushable.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected boolean afterCopy(LocalFileInstance fileInstance) {
        return true;
    }

    @Override
    public boolean doWork() {
        try {
            if (fileInstance.isFile()) {
                return copyFileTo(fileInstance, destination);
            } else {
                return copyDirectoryTo(fileInstance, destination);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (fileOperateListener != null) {
                fileOperateListener.onError(new Message("出现异常" + e.getMessage()), FileInstance.file_operate_type_copy);
            }
        }
        return false;
    }

    boolean copyDirectoryTo(LocalFileInstance currentDirectory, LocalFileInstance to) throws Exception {
        FilesAndDirectories list = currentDirectory.listSafe();
        List<FileItemModel> files = list.getFiles();
        for (FileItemModel file : files) {
            if (isNeedStopByNext()) break;
            String name = file.getName();
            LocalFileInstance originFile = (LocalFileInstance) currentDirectory.toChild(name, true, false);
            boolean f = copyFileTo(originFile, to);
            if (fileOperateListener != null) {
                if (f) {
                    fileOperateListener.onOneFile(file.getAbsolutePath(), file.getName(), originFile.getFileLength(), FileInstance.file_operate_type_copy,
                            new Message("成功复制一个文件"+file.getName()).add(file.getAbsolutePath()));
                } else {
                    fileOperateListener.onError(
                            new Message("复制文件失败" + file.getName()).add(file.getAbsolutePath()), FileInstance.file_operate_type_copy);
                }
            }
            if (!f) {
                return false;
            }
        }
        List<DirectoryItemModel> directories = list.getDirectories();
        for (DirectoryItemModel directory : directories) {
            if (isNeedStopByNext()) break;
            //应该获取新的路径
            String name = directory.getName();
            LocalFileInstance currentSub = (LocalFileInstance) currentDirectory.toChild(name, false, false);
            LocalFileInstance toSub = (LocalFileInstance) to.toChild(name, false, true);
            boolean d = copyDirectoryTo(currentSub, toSub);
            if (fileOperateListener != null) {
                if (d) {
                    fileOperateListener.onOneDirectory(directory.getAbsolutePath(), directory.getName(), FileInstance.file_operate_type_copy,
                            new Message("成功复制一个文件夹"+directory.getName()).add(directory.getAbsolutePath()));
                } else {
                    fileOperateListener.onError(
                            new Message("复制文件失败" + directory.getName()).add(directory.getAbsolutePath()), FileInstance.file_operate_type_copy);
                }
            }
            if (!d) {
                return false;
            }
        }
        if (fileOperateListener != null) {
            fileOperateListener.onOneDirectory(currentDirectory.getPath(), currentDirectory.getName(), FileInstance.file_operate_type_copy,
                    new Message("文件夹任务完成"+currentDirectory.getName()).add(currentDirectory.getPath()));
        }
        return afterCopyDirectory(currentDirectory);
    }

    protected boolean afterCopyDirectory(FileInstance currentDirectory) {
        return true;
    }
}
